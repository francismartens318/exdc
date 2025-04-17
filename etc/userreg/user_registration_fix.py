#!/usr/bin/env python3
# Script to fix user registration issues by:
# 1. Adding a new user to the database
# 2. Updating lifecycle info
# 3. Triggering verification workflow
#
# Usage: python3 user_registration_fix.py
#
# Requirements: 
# - psycopg2 (pip install psycopg2-binary)
# - requests (pip install requests)
#
# Note: This script assumes the PostgreSQL database is running locally
# on port 5432 with the credentials from the docker-compose.yml file.
import hashlib
import psycopg2
import requests
import getpass
import sys
import json

def hash_password(password):
    """Convert password to SHA256 hash"""
    hash_object = hashlib.sha256(password.encode())
    return hash_object.hexdigest()

def collect_input_data():
    """Collect email, password, and instanceUID from user"""
    print("=== Collecting User Data ===")
    email = input("Enter email address: ")
    password = getpass.getpass("Enter password: ")
    instance_uid = input("Enter instanceUID: ")

    return email, password, instance_uid

def connect_to_database():
    """Connect to the PostgreSQL database"""
    print("\n=== Connecting to Database ===")
    try:
        conn = psycopg2.connect(
            host="localhost",
            port=5432,
            database="ccnode",
            user="exalate",
            password="exalate"
        )
        print("Database connection successful")
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        sys.exit(1)

def insert_user(conn, email, password_hash):
    """Insert a new user into the users table"""
    print("\n=== Inserting New User ===")
    try:
        cursor = conn.cursor()

        # Check if user already exists
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        existing_user = cursor.fetchone()

        if existing_user:
            print(f"User with email {email} already exists with ID {existing_user[0]}")
            return existing_user[0]

        # Insert new user
        cursor.execute(
            "INSERT INTO users (email, credentials, status) VALUES (%s, %s, 'ACTIVE') RETURNING id",
            (email, password_hash)
        )
        user_id = cursor.fetchone()[0]
        conn.commit()

        print(f"User inserted successfully with ID {user_id}")
        return user_id
    except Exception as e:
        conn.rollback()
        print(f"Error inserting user: {e}")
        sys.exit(1)

def update_lifecycle_info(conn, user_id):
    """Update the lifecycle_info table"""
    print("\n=== Updating Lifecycle Info ===")
    try:
        cursor = conn.cursor()



        # Update lifecycle info
        cursor.execute(
            "UPDATE lifecycle_info SET is_eula_accepted = true, verified = true WHERE id=1",
            (user_id,)
        )
        conn.commit()

        print("Lifecycle info updated successfully")
    except Exception as e:
        conn.rollback()
        print(f"Error updating lifecycle info: {e}")
        sys.exit(1)

def send_verification_email(email, instance_uid):
    """Send a REST request to trigger the verification workflow"""
    print("\n=== Sending Verification Email ===")
    url = "https://licensegen.exalate.com/rest/scriptrunner/latest/custom/sendVerificationEmail"

    payload = {
        "exalateUrl": "http://localhost:9002",
        "instanceUID": instance_uid,
        "trackerUrl": "https://community.exalate.st",
        "trackerType": "DISCOURSE",
        "requestUserMail": email,
        "contactName": email,
        "organization": "exalate",
        "phoneNumber": "+380916111102"
    }

    try:
        response = requests.post(url, json=payload)
        print(f"Response status code: {response.status_code}")

        if response.status_code == 200:
            print("Verification email sent successfully")
            try:
                print(f"Response: {json.dumps(response.json(), indent=2)}")
            except:
                print(f"Response: {response.text}")
        else:
            print(f"Failed to send verification email: {response.text}")
    except Exception as e:
        print(f"Error sending verification email: {e}")

def main():
    print("=== User Registration Fix Script ===")

    # Step 1: Collect input data
    email, password, instance_uid = collect_input_data()
    password_hash = hash_password(password)

    # Step 2: Connect to database
    conn = connect_to_database()

    # Step 3: Insert user
    user_id = insert_user(conn, email, password_hash)

    # Step 4: Update lifecycle info
    update_lifecycle_info(conn, user_id)

    # Step 5: Send verification email
    send_verification_email(email, instance_uid)

    # Close database connection
    conn.close()

    print("\n=== Script Execution Complete ===")

if __name__ == "__main__":
    main()
