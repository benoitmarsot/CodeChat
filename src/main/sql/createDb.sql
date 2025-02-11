CREATE DATABASE codechat;
CREATE USER ccadmin WITH ENCRYPTED PASSWORD 'ght5opn';
ALTER ROLE ccadmin SET client_encoding TO 'utf8';
ALTER ROLE ccadmin SET default_transaction_isolation TO 'read committed';
ALTER ROLE ccadmin SET timezone TO 'UTC';
GRANT ALL PRIVILEGES ON DATABASE codechat TO ccadmin;
USE codechat;
CREATE SCHEMA core AUTHORIZATION ccadmin;