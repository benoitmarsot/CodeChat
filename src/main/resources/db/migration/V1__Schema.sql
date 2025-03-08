-- Create core schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS core;

-- Set search path to include core schema
SET search_path TO core, public;
