#!/usr/bin/env bash

echo "We will create a database for Postgresql"
read -p "Database name:" DB_NAME
read -p "Database username:" DB_USERNAME
read -p "Database password:" DB_PASSWORD


# Creating database with Postgresql
sudo echo -e "

DROP DATABASE IF EXISTS \"$DB_NAME\";
DROP USER IF EXISTS \"$DB_USERNAME\";

CREATE DATABASE \"$DB_NAME\";
CREATE USER \"$DB_USERNAME\" WITH PASSWORD '$DB_PASSWORD';
ALTER ROLE \"$DB_USERNAME\" SET client_encoding TO 'utf8';
ALTER ROLE \"$DB_USERNAME\" SET default_transaction_isolation TO 'read committed';
ALTER ROLE \"$DB_USERNAME\" SET timezone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE \"$DB_NAME\" TO \"$DB_USERNAME\";
" > create.sql

sudo -u postgres psql -f create.sql
rm create.sql

sudo -u postgres PGPASSWORD=$DB_PASSWORD psql -U $DB_USERNAME -h 127.0.0.1 -d $DB_NAME -a -f timestamp-server-postgres.sql

# register datasource with wildfly
