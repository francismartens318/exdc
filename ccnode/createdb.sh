#!/bin/bash
  
TEST=`psql -U postgres <<-EOSQL
   SELECT 1 FROM pg_database WHERE datname='$DB_NAME';
EOSQL`
  
echo "******CREATING DOCKER DATABASE******"
if [[ $TEST == "1" ]]; then
    # database exists
    # $? is 0
    exit 0
else
psql -U postgres <<-EOSQL
   CREATE ROLE $DB_USER WITH LOGIN ENCRYPTED PASSWORD '${DB_PASS}' SUPERUSER;
EOSQL
  
psql -U postgres <<-EOSQL
   CREATE DATABASE $DB_NAME WITH OWNER $DB_USER ENCODING 'UNICODE' LC_COLLATE 'C' LC_CTYPE 'C' TEMPLATE template0;
EOSQL
  
psql -U postgres <<-EOSQL
   GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
EOSQL
fi
  
echo ""
echo "******DOCKER DATABASE CREATED******"

