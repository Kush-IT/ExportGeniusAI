import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.ensemble import GradientBoostingRegressor
import joblib
from features import get_preprocessor

def train_model():
    # Load dataset
    df = pd.read_csv('training_deals.csv')
    
    # Split into features (X) and target (y)
    X = df[['category', 'destination_country', 'supply_price', 'quantity']]
    y = df['margin_pct']
    
    # Train test split
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Assemble pipeline
    preprocessor = get_preprocessor()
    model = GradientBoostingRegressor(
        n_estimators=150,
        max_depth=3,
        learning_rate=0.08,
        random_state=42
    )
    
    pipeline = Pipeline(steps=[
        ('preprocessor', preprocessor),
        ('regressor', model)
    ])
    
    # Fit the pipeline
    print("Fitting model...")
    pipeline.fit(X_train, y_train)
    print("Model fitted successfully!")
    
    # Save the pipeline
    joblib.dump(pipeline, 'pricing_model.pkl')
    print("Model saved to pricing_model.pkl.")
    
if __name__ == '__main__':
    train_model()
