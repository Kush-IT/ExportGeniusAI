import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score
import joblib

def evaluate():
    # Load dataset
    df = pd.read_csv('training_deals.csv')
    
    X = df[['category', 'destination_country', 'supply_price', 'quantity']]
    y = df['margin_pct']
    
    # 80/20 split
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # Load model
    model = joblib.load('pricing_model.pkl')
    
    # Predict
    y_pred = model.predict(X_test)
    
    # Compute metrics
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    
    print("================ MODEL EVALUATION ================")
    print(f"Model Mean Absolute Error (MAE): {mae:.5f}")
    print(f"Model R² Score:                  {r2:.5f}")
    
    # Naive baseline: Category-mean margin prediction
    train_df = pd.concat([X_train, y_train], axis=1)
    category_means = train_df.groupby('category')['margin_pct'].mean().to_dict()
    
    # Predict using naive category means
    y_naive = X_test['category'].map(category_means).fillna(y_train.mean())
    naive_mae = mean_absolute_error(y_test, y_naive)
    
    print(f"Naive Baseline MAE:              {naive_mae:.5f}")
    print("-------------------------------------------------")
    
    if mae < naive_mae:
        print("SUCCESS: The model outperforms the naive baseline.")
    else:
        print("WARNING: The model fails to beat the naive baseline. Retraining is recommended.")
    print("=================================================")

if __name__ == '__main__':
    evaluate()
