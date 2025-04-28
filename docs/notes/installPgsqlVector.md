# Installing pgvector

Try in your sql editor first:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

If it doesn't work

## Installing pgvector Extension with Homebrew

The command CREATE EXTENSION IF NOT EXISTS vector; doesn't work directly with Homebrew because you need to install the pgvector extension first. Here's how to properly set up pgvector with a Homebrew-installed PostgreSQL:

### 1. Install pgvector using Homebrew:

```powershell
brew install pgvector
```

### 2. Link to your PostgreSQL installation:

```powershell
brew link --force pgvector
```

### 3.Verify the installation location:

```powershell
brew info pgvector
```

Note the installation path (typically something like /opt/homebrew/opt/pgvector)

### 4. Copy extension files to PostgreSQL:

_I did not need to do that..._

```powershell
# Replace with your PostgreSQL version (e.g., 15)
cp /opt/homebrew/opt/pgvector/lib/postgresql/vector.so /opt/homebrew/opt/postgresql@15/lib/
cp /opt/homebrew/opt/pgvector/share/postgresql/extension/* /opt/homebrew/opt/postgresql@15/share/postgresql/extension/
```

### 5. Connect to PostgreSQL and create the extension:

```powershell
psql your_database_name
```

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

## Alternative: Build from Source

If the Homebrew installation isn't working:

```powershell
git clone https://github.com/pgvector/pgvector.git
cd pgvector
make
make install
```

Then run the CREATE EXTENSION command.

Make sure you're running the command as a user with sufficient privileges (typically the postgres user or a superuser).

## Give the permission to ccadmin

```sql
-- Grant USAGE on the schema where vector is installed (usually public)
GRANT USAGE ON SCHEMA public TO ccadmin;

-- Grant USAGE on the vector type
GRANT USAGE ON TYPE vector TO ccadmin;

-- Optionally, grant EXECUTE on vector functions (if needed)
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ccadmin;
```

## ccadmin needs the search path to access the operator and, type vector

```sql
SET search_path TO core, public;
```

_I'll install it in our db script_

## Verify it is installed

```sql
codechat=# SELECT * FROM pg_extension;
  oid   | extname | extowner | extnamespace | extrelocatable | extversion | extconfig | extcondition
--------+---------+----------+--------------+----------------+------------+-----------+--------------
  14028 | plpgsql |       10 |           11 | f              | 1.0        |           |
 470657 | vector  |       10 |         2200 | t              | 0.8.0      |           |
(2 rows)
```

### Test Functionality

```sql
-- Create a test table with a vector column
CREATE TABLE vector_test (
  id serial PRIMARY KEY,
  embedding vector(3)
);

-- Insert a test vector
INSERT INTO vector_test (embedding) VALUES ('[1,2,3]');

-- Query using vector functions
SELECT * FROM vector_test WHERE embedding <-> '[3,2,1]' < 5;

-- Clean up
DROP TABLE vector_test;
```
