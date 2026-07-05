import csv
import random
import numpy as np

# Seed for reproducibility
random.seed(42)
np.random.seed(42)

categories = ['Textiles', 'Electronics', 'Spices', 'Handicrafts', 'Chemicals']
countries = ['USA', 'Germany', 'UAE', 'UK', 'Australia', 'Japan']

category_margins = {
    'Textiles': 0.35,
    'Electronics': 0.22,
    'Spices': 0.55,
    'Handicrafts': 0.48,
    'Chemicals': 0.28
}

country_multipliers = {
    'USA': 1.15,
    'Germany': 1.10,
    'UAE': 1.05,
    'UK': 1.12,
    'Australia': 1.08,
    'Japan': 1.18
}

def generate_data(num_rows=2000):
    with open('training_deals.csv', mode='w', newline='') as file:
        writer = csv.writer(file)
        # Header
        writer.writerow(['category', 'destination_country', 'supply_price', 'quantity', 'margin_pct', 'sell_price'])
        
        for _ in range(num_rows):
            category = random.choice(categories)
            country = random.choice(countries)
            
            # Supply price varies based on category
            if category == 'Electronics':
                supply_price = round(random.uniform(50.0, 1000.0), 2)
                quantity = random.randint(10, 500)
            elif category == 'Chemicals':
                supply_price = round(random.uniform(10.0, 200.0), 2)
                quantity = random.randint(100, 2000)
            elif category == 'Spices':
                supply_price = round(random.uniform(2.0, 50.0), 2)
                quantity = random.randint(500, 5000)
            else: # Textiles, Handicrafts
                supply_price = round(random.uniform(5.0, 150.0), 2)
                quantity = random.randint(100, 1000)
                
            # Base margin
            base_margin = category_margins[category]
            multiplier = country_multipliers[country]
            
            # Compute margin with multiplier and Gaussian noise
            noise = np.random.normal(0, 0.05)
            margin_pct = (base_margin * multiplier) + noise
            
            # Clamp to [0.10, 0.70]
            margin_pct = max(0.10, min(0.70, margin_pct))
            margin_pct = round(margin_pct, 4)
            
            # Calculate sell price: sell_price = supply_price / (1 - margin_pct)
            sell_price = round(supply_price / (1.0 - margin_pct), 2)
            
            writer.writerow([category, country, supply_price, quantity, margin_pct, sell_price])

if __name__ == '__main__':
    print("Generating synthetic dataset of 2000 deals...")
    generate_data()
    print("Dataset saved to training_deals.csv.")
