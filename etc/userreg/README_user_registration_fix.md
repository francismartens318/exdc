# User Registration Fix Script

This script helps circumvent a bug in the application by:
1. Adding a new user to the database
2. Updating lifecycle info
3. Triggering the verification workflow

## Prerequisites

- Python 3.x
- PostgreSQL database running (as configured in docker-compose.yml)
- Required Python packages:
  - psycopg2-binary
  - requests

## Installation

1. Make sure you have Python 3.x installed:
   ```
   python3 --version
   ```

2. Ensure pip (Python package installer) is installed:

   If pip is not installed, you can install it using:

   **For Linux/Debian/Ubuntu:**
   ```
   sudo apt update
   sudo apt install python3-pip
   ```

   **For macOS:**
   ```
   curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
   python3 get-pip.py
   ```

   **For Windows:**
   ```
   curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
   python get-pip.py
   ```

   Verify pip installation:
   ```
   pip --version
   ```

3. Install the required Python packages:
   ```
   pip install psycopg2-binary requests
   ```

## Usage

1. Navigate to the script directory:
   ```
   cd /Users/francis/workspace/exdc/etc/
   ```

2. Run the script:
   ```
   python3 user_registration_fix.py
   ```

3. Follow the prompts to enter:
   - Email address
   - Password (will not be displayed as you type)
   - Instance UID

## What the Script Does

1. **Collects User Data**: Prompts for email, password, and instanceUID
2. **Connects to Database**: Connects to the PostgreSQL database running on localhost:5432
3. **Inserts New User**: Adds a user with the provided email, SHA256 hashed password, and ACTIVE status
4. **Updates Lifecycle Info**: Sets is_eula_accepted and verified to true in the life_cycle_info table
5. **Sends Verification Email**: Triggers the verification workflow by sending a REST request

## Troubleshooting

- **Database Connection Issues**: Ensure the PostgreSQL database is running and accessible on localhost:5432
- **Missing Dependencies**: If you get import errors, make sure you've installed the required packages
- **Pip Not Found**: If you get "pip: command not found" error, follow the pip installation instructions in the Installation section
- **Pip Installation Errors**: If you encounter issues installing pip, try using the system package manager (apt, brew, etc.) or check Python's official documentation
- **Permission Issues**: If you can't run the script, try using `sudo python3 user_registration_fix.py`

## Notes

- The script assumes the PostgreSQL database is running locally on port 5432 with the credentials from the docker-compose.yml file
- The script includes error handling and will display detailed error messages if something goes wrong
- If a user with the provided email already exists, the script will use that user instead of creating a new one
