import unittest
import json
import os
import sys

# Ensure pharmacy-ml is on sys.path
sys.path.insert(0, os.path.dirname(__file__))

# Load .env if present
env_path = os.path.join(os.path.dirname(__file__), "..", ".env")
if os.path.exists(env_path):
    for line in open(env_path):
        line = line.strip()
        if line and not line.startswith("#") and "=" in line:
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip().strip("'").strip('"'))

from demand_predictor import app, train_and_evaluate, load_sales_data

class DemandPredictorTestCase(unittest.TestCase):
    def setUp(self):
        self.app = app.test_client()
        self.app.testing = True

    def test_health_check_endpoint(self):
        response = self.app.get('/')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data.decode('utf-8'))
        self.assertEqual(data.get('status'), 'ONLINE')
        self.assertIn(data.get('database'), ['MongoDB Atlas', 'HISTORICAL_ENRICHED (MongoDB not configured)'])

    def test_predictions_endpoint(self):
        response = self.app.get('/api/predictions')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.data.decode('utf-8'))
        self.assertIn('predictions', data)
        self.assertIn('metrics', data)
        self.assertTrue(len(data['predictions']) > 0)
        first_pred = data['predictions'][0]
        self.assertIn('medicineName', first_pred)
        self.assertIn('predicted30DayDemand', first_pred)
        self.assertIn('recommendedReorderQuantity', first_pred)
        self.assertIn('status', first_pred)

    def test_model_training_and_metrics(self):
        metrics, predictions = train_and_evaluate()
        self.assertIn('randomForest', metrics)
        self.assertIn('linearRegression', metrics)
        self.assertIn('R2', metrics['randomForest'])
        self.assertIn('MAE', metrics['randomForest'])
        self.assertIn('RMSE', metrics['randomForest'])
        self.assertTrue(len(predictions) > 0)
        self.assertIn(metrics['bestModel'], ['RandomForestRegressor', 'LinearRegression'])

if __name__ == '__main__':
    unittest.main()
