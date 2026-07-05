from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import OneHotEncoder

def get_preprocessor():
    return ColumnTransformer(
        transformers=[
            ('cat', OneHotEncoder(handle_unknown='ignore'), ['category', 'destination_country']),
            ('num', 'passthrough', ['supply_price', 'quantity'])
        ]
    )
