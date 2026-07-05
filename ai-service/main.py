import os
import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

app = FastAPI(title="ExportGenius AI Service")

# Load model pipeline
MODEL_PATH = "pricing_model.pkl"
model = None
if os.path.exists(MODEL_PATH):
    try:
        model = joblib.load(MODEL_PATH)
        print("AI model loaded successfully.")
    except Exception as e:
        print(f"Error loading model: {e}")
else:
    print("Model file pricing_model.pkl not found. Run training script first.")

# Database setup
DATABASE_URL = os.environ.get(
    "DATABASE_URL", 
    "postgresql://postgres:kush%40patel06@db.wlqlwdswhmrvbmxaqxey.supabase.co:5432/postgres"
)

db_connected = False
SessionLocal = None

try:
    engine = create_engine(DATABASE_URL, connect_args={"connect_timeout": 3})
    # Test connection
    with engine.connect() as conn:
        conn.execute(text("SELECT 1"))
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    db_connected = True
    print("AI Service connected to database successfully.")
except Exception as e:
    print(f"Database connection failed: {e}. AI service starting in fallback mock mode.")

class SuggestPricingRequest(BaseModel):
    supply_price: float
    category: str
    destination_country: str
    quantity: int

@app.get("/health")
def health_check():
    return {"status": "ok", "database_connected": db_connected}

@app.post("/pricing/suggest")
def suggest_pricing(request: SuggestPricingRequest):
    global model
    if model is None:
        # Emergency hardcoded fallback if model file is missing
        base_margins = {
            'Textiles': 0.35,
            'Electronics': 0.22,
            'Spices': 0.55,
            'Handicrafts': 0.48,
            'Chemicals': 0.28
        }
        margin_pct = base_margins.get(request.category, 0.25)
    else:
        try:
            # Prepare DataFrame for model prediction
            input_df = pd.DataFrame([{
                "category": request.category,
                "destination_country": request.destination_country,
                "supply_price": request.supply_price,
                "quantity": request.quantity
            }])
            margin_pct = float(model.predict(input_df)[0])
        except Exception as e:
            # Clamps to default on error
            margin_pct = 0.25

    # Clamp to [0.10, 0.70]
    margin_pct = max(0.10, min(0.70, margin_pct))
    
    suggested_sell_price = round(request.supply_price / (1.0 - margin_pct), 2)
    margin_amount = round(suggested_sell_price - request.supply_price, 2)
    
    return {
        "suggested_sell_price": suggested_sell_price,
        "predicted_margin_pct": round(margin_pct, 4),
        "margin_amount": margin_amount
    }

@app.get("/pricing/margin-summary")
def get_margin_summary():
    if not db_connected or SessionLocal is None:
        # Fallback Mock margins
        return [
            {"category": "Textiles", "avg_margin_pct": 0.3512},
            {"category": "Electronics", "avg_margin_pct": 0.2185},
            {"category": "Spices", "avg_margin_pct": 0.5472},
            {"category": "Handicrafts", "avg_margin_pct": 0.4795},
            {"category": "Chemicals", "avg_margin_pct": 0.2789}
        ]

    db = SessionLocal()
    try:
        query = text("""
            SELECT c.name as category, AVG(m.margin_pct) as avg_margin_pct
            FROM margin_ledger m
            JOIN deals d ON m.deal_id = d.id
            JOIN exporter_catalogue cat ON d.catalogue_id = cat.id
            JOIN categories c ON cat.category_id = c.id
            GROUP BY c.name
        """)
        results = db.execute(query).fetchall()
        return [{"category": r[0], "avg_margin_pct": float(r[1])} for r in results]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()

@app.get("/matching/suggest/{requirement_id}")
def suggest_matches(requirement_id: str):
    if not db_connected or SessionLocal is None:
        # Fallback matching suggestions if database is offline
        return [
            {
                "catalogue_id": "d3b07384-d113-4ec2-a5d6-c4d5e6f7a8b1",
                "title": "Premium Spices Candidate A",
                "supply_price": 4.50,
                "score": 87.50,
                "exporter_name": "Mock Exporter A"
            },
            {
                "catalogue_id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                "title": "Spices Candidate B",
                "supply_price": 5.20,
                "score": 79.40,
                "exporter_name": "Mock Exporter B"
            }
        ]

    db = SessionLocal()
    try:
        # 1. Fetch requirement details
        req_query = text("SELECT product_type, destination_country, quantity, target_price FROM requirements WHERE id = :id")
        req = db.execute(req_query, {"id": requirement_id}).fetchone()
        if not req:
            raise HTTPException(status_code=404, detail="Requirement not found")
        
        product_type, dest_country, req_qty, target_price = req
        
        # 2. Fetch active catalog entries
        cat_query = text("""
            SELECT id, title, category_id, supply_price, lead_time_days, exporter_id
            FROM exporter_catalogue
            WHERE is_active = true
        """)
        cat_entries = db.execute(cat_query).fetchall()

        # 3. Fetch reliability scores
        scores_query = text("SELECT exporter_id, qa_pass_pct, on_time_pct FROM reliability_scores")
        scores_list = db.execute(scores_query).fetchall()
        reliability_map = {r[0]: (float(r[1]) + float(r[2])) / 200.0 for r in scores_list} # mapped to [0,1]

        candidates = []
        for entry in cat_entries:
            cat_id, title, cat_category_id, supply_price, lead_time, exporter_id = entry
            supply_price = float(supply_price)
            
            # A. Category / Product Fit (40%)
            # Full fit if product type in title
            fit_score = 0.0
            if product_type.lower() in title.lower() or title.lower() in product_type.lower():
                fit_score = 1.0
            elif len(set(product_type.lower().split()) & set(title.lower().split())) > 0:
                fit_score = 0.6
            else:
                fit_score = 0.2

            # B. Reliability (25%)
            rel_score = reliability_map.get(exporter_id, 0.8) # Default to 80% if no score exists

            # C. Price Competitiveness (20%)
            # Lower supply price relative to target price scores higher
            price_score = 0.5
            if target_price:
                target_val = float(target_price)
                if supply_price <= target_val:
                    price_score = 1.0
                else:
                    # Penalty for exceeding target
                    price_score = max(0.0, 1.0 - (supply_price - target_val) / target_val)

            # D. Lead Time (15%)
            # Shorter is better. Mapped to [0,1] with a base of 30 days
            lead_score = max(0.0, 1.0 - (lead_time / 30.0))

            # Weighted sum
            weighted_score = (fit_score * 0.40) + (rel_score * 0.25) + (price_score * 0.20) + (lead_score * 0.15)
            final_score_pct = round(weighted_score * 100.0, 2)

            candidates.append({
                "catalogue_id": str(cat_id),
                "title": title,
                "supply_price": supply_price,
                "score": final_score_pct
            })

        # Sort by score descending and return top 3
        candidates = sorted(candidates, key=lambda x: x["score"], reverse=True)[:3]
        return candidates

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        db.close()
