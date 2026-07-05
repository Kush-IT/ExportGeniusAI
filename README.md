# ExportGenius AI - Trade Brokerage Platform

ExportGenius AI is a secure, AI-powered trade brokerage platform that bridges the gap between exporters and importers while maintaining strict privacy. Importers and Exporters act independently and never see each other's identity or raw catalog prices — ExportGenius AI serves as the sole counterparty to both sides via an Admin role.

---

## Technical Stack
- **Backend**: Spring Boot 3.2.4 (Java 17) + Spring Security (JWT) + JPA/Hibernate
- **Database**: PostgreSQL 16
- **AI Microservice**: Python FastAPI + Scikit-Learn (Gradient Boosting Regressor) + SQLAlchemy
- **Frontend Console**: Glassmorphism Single-Page Application (HTML5 / Vanilla CSS / Vanilla JS) served directly from the Spring Boot static resource handler.

---

## Project Structure
```
├── backend/                       # Spring Boot Java Application
│   ├── src/main/java/             # Core JPA entities, repositories, controllers, clients
│   ├── src/main/resources/static/ # Glassmorphic SPA Dashboard (index.html, app.js, index.css)
│   ├── src/test/java/             # Web API Integration & PII Redaction Unit Tests
│   ├── Dockerfile
│   └── pom.xml
├── ai-service/                    # Python FastAPI AI Microservice
│   ├── main.py                    # Endpoint controllers (/pricing/suggest, /matching/suggest)
│   ├── features.py                # scikit-learn ColumnTransformer
│   ├── generate_training_data.py  # Produces 2,000 row synthetic trade dataset
│   ├── train_pricing_model.py     # Fits Gradient Boosting Regressor on synthetic data
│   ├── evaluate_model.py          # Validation script (compares model to naive category baseline)
│   ├── Dockerfile
│   └── requirements.txt
├── database/
│   ├── schema.sql                 # Complete 23-table Postgres schema definitions
│   └── seed.sql                   # Seeds system roles and item categories
└── docker-compose.yml             # Global container configuration orchestrator
```

---

## Quick Start (Local Development)

### 1. Database Setup
Boot the Postgres container:
```powershell
docker-compose up -d db
```
The database will automatically initialize the schema from `/database/schema.sql` and populate records from `/database/seed.sql` on startup.

### 2. Run the AI Service
To run the AI microservice locally:
1. Navigate to `ai-service/`
2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Generate the training dataset and fit the model:
   ```bash
   python generate_training_data.py
   python train_pricing_model.py
   ```
   *Optional:* Verify the model metrics by running `python evaluate_model.py`.
4. Start the FastAPI microservice on port 8000:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```

### 3. Run the Java Backend
To run the Spring Boot app:
1. Navigate to `backend/`
2. Verify you have Java 17 and build/run the application:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; mvn spring-boot:run
   ```
3. Open your browser and navigate to:
   **[http://localhost:8080](http://localhost:8080)**

---

## Full Docker Compose Run
To launch all services (Database, AI Service, and Spring Boot Backend) compiled and linked together inside Docker:
```bash
# 1. Package the backend JAR locally
cd backend
mvn clean package -DskipTests

# 2. Start all containers from the root directory
cd ..
docker-compose up --build
```
Once healthy, access the web console at `http://localhost:8080`.

---

## Testing & Verification

### Running Tests
Execute the unit and integration suite:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; mvn test
```

### Key Test Deliverables
1. **`DtoPiiSeparationTest.java`**: Reflectively asserts that data transfer views exposed to exporters and importers remain isolated and do not leak counterparty field mappings.
2. **`PdfRedactionTest.java`**: Generates trade PDFs (Invoice, PO, QA Certificate, Trade Agreement) using distinct mock fixtures and uses Apache PDFBox to assert that counterparty PII is strictly redacted in customer-facing files.
3. **`ResilienceFallbackTest.java`**: Simulates an unreachable AI pricing endpoint to assert that the circuit breaker failover calculates 90-day category average margins or defaults to a flat 25% margin.
