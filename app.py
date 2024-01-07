import pickle
from sklearn.impute import SimpleImputer

from flask import Flask, request, jsonify, render_template
import joblib
import cv2
from skimage.transform import resize
import numpy as np
from io import BytesIO
from PIL import Image
import requests
import torch
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import GridSearchCV
from tqdm import tqdm

app = Flask(__name__)

# Load the pre-trained model
with open('C:/Users/HP/PycharmProjects/isproject/model.pkl', 'rb') as model_file:
    best_forest = pickle.load(model_file)


# Function to fetch and preprocess satellite image
# Function to fetch satellite image
def fetch_satellite_image(lat, lon, zoom_level, api_key, save_path, resize_to=None):
    base_url = 'https://maps.googleapis.com/maps/api/staticmap?'
    params = {
        'center': f"{lat},{lon}",
        'size': '640x640',
        'zoom': zoom_level,
        'maptype': 'satellite',
        'key': api_key
    }

    try:
        response = requests.get(base_url, params=params)
        response.raise_for_status()

        if resize_to:
            img = Image.open(BytesIO(response.content))
            img = img.resize(resize_to)
            img.save(save_path)
        else:
            with open(save_path, 'wb') as f:
                f.write(response.content)

        return True
    except Exception as e:
        print(f"Error fetching image for coordinates ({lat}, {lon}): {str(e)}")
        return False


def fetch_and_preprocess_image(lat, lon, zoom_level, api_key):
    save_path = "C:/Users/HP/PycharmProjects/isproject/input_image.png"
    fetch_satellite_image(lat, lon, zoom_level, api_key, save_path, resize_to=(256, 256))
    input_image = cv2.imread(save_path)
    if input_image is None:
        return jsonify({'error': 'Failed to fetch or read image'}), 500
    resized_image = resize(input_image, (256, 256, 3), anti_aliasing=True)
    return torch.tensor(np.array(resized_image) / 255.0, dtype=torch.float32).view(1, -1)




# Function to make predictions



def preprocess_input(lat, lon, housing_age, total_rooms, total_bedrooms, population, median_income, households):
    # Fetch satellite image
    zoom_level = 19
    api_key = 'AIzaSyA0Mtj2S3EiqW-ng1wtyJSFnrguXDXUl7c'
    image_path = "C:/Users/HP/PycharmProjects/isproject/input_image.png"
    fetch_satellite_image(lat, lon, zoom_level, api_key, image_path, resize_to=(256, 256))

    # Load and preprocess the image
    input_image = cv2.imread(image_path)
    if input_image is None:
        return jsonify({'error': 'Failed to fetch or read image'}), 500
    resized_image = resize(input_image, (256, 256, 3), anti_aliasing=True)
    flat_image = torch.tensor(np.array(resized_image) / 255.0, dtype=torch.float32).view(1, -1)
    bedroom_ratio = total_bedrooms / total_rooms
    household_rooms = total_rooms / households

    # Prepare input data
    input_data = torch.tensor([
        lat, lon, housing_age, total_rooms, total_bedrooms, population, median_income, households, bedroom_ratio, household_rooms
    ], dtype=torch.float32).view(1, -1)

    # Concatenate features and flattened image
    input_features = torch.cat((input_data, flat_image), dim=1)
    # Check the number of features
    expected_features = 196618  # Replace with the actual expected number
    current_features = input_features.size(1)

    if current_features != expected_features:
        return jsonify({'error': f'Expected {expected_features} features, but got {current_features}'}), 500

    return input_features



def preprocess_and_predict(data):
    # Assuming data is a dictionary with keys corresponding to input features
    lat, lon, housing_age, total_rooms, total_bedrooms, population, median_income, households = (
        data['latitude'], data['longitude'], data['housing_median_age'], data['total_rooms'],
        data['total_bedrooms'], data['population'], data['median_income'], data['households']
    )

    # Preprocess input data
    input_features = preprocess_input(lat, lon, housing_age, total_rooms, total_bedrooms, population, median_income,
                                      households)

    # Make predictions using the loaded model
    predictions = best_forest.predict(input_features)

    return predictions


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/predict', methods=['POST'])
def predict():
    try:
        # Get input data from JSON request
        input_data = request.json

        # Print the shape of the input_data
        print("Shape of input_data:", np.array(input_data).shape)

        # Make prediction using the model
        prediction = preprocess_and_predict(input_data)

        return jsonify({'prediction': prediction.tolist()})

    except Exception as e:
        return jsonify({'error': str(e)})


if __name__ == '__main__':
    app.run(debug=True, port=8000)
