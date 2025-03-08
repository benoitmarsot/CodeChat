CREATE USER ${DB_CODECHAT_USERNAME} WITH ENCRYPTED PASSWORD '${DB_CODECHAT_PASSWORD}';
ALTER ROLE ${DB_CODECHAT_USERNAME} SET client_encoding TO 'utf8';
ALTER ROLE ${DB_CODECHAT_USERNAME} SET default_transaction_isolation TO 'read committed';
ALTER ROLE ${DB_CODECHAT_USERNAME} SET timezone TO 'UTC';
CREATE DATABASE ${DB_CODECHAT_NAME} WITH OWNER = ${DB_CODECHAT_USERNAME} ENCODING = 'UTF8' CONNECTION LIMIT = -1;
\c ${DB_CODECHAT_NAME};
CREATE SCHEMA core AUTHORIZATION ${DB_CODECHAT_USERNAME};
ALTER ROLE ${DB_CODECHAT_USERNAME} SET search_path TO core;

-- db creation script
-- cat createDb.template.sql | envsubst > createDb.sql
-- psql -U benoitmarsot -d postgres -f createDb.sql
