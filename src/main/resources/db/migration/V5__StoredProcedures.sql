SET search_path TO ${schema};

-- Functions

-- Create a user
-- Example:
-- call createuser(
--     '{"id":0,"name":"Josie Marsot","email":"josiemarsot@hotmail.com"}'::json
-- )
DROP PROCEDURE IF EXISTS core.createuser;
CREATE OR REPLACE PROCEDURE core.createuser(
    jsonUser json,
    INOUT out_userid int DEFAULT NULL
) AS $$
DECLARE
    v_name varchar(50);
    v_email varchar(100);
    v_role varchar(20);
    v_created timestamp;
BEGIN
    -- check if the user is already registered
    IF EXISTS(SELECT 1 FROM core.users u WHERE u.email = jsonUser->>'email') THEN 
        RAISE EXCEPTION 'User with email % already exist.', jsonUser->>'email'
            USING HINT = 'Please login in instead.';
    END IF;
    
    -- create the user
    INSERT INTO core.users (
        name, email, password, role
    ) VALUES (
        jsonUser->>'name',
        jsonUser->>'email',
        jsonUser->>'password',
        jsonUser->>'role'
    ) RETURNING 
        users.userid,
        users.name,
        lower(users.email),
        users.created
    INTO 
        out_userid,
        v_name,
        v_email,
        v_created;
        
    RAISE NOTICE 'Created user: % name: % with email: % and role: % on %', out_userid, v_name, v_email, v_role, v_created;
END;
$$ LANGUAGE plpgsql;

-- Create a OaiFile
-- Example:
-- call createoaifile('{"fileId":"file-G8esRwhimXuLiYXVd5RuXE","fileName":"Recovery.java","rootdir":"/Users/benoitmarsot/dealerfx/dev/dbtools/java/dbtools-base/dbcompare/src/main/java/dev/platform5/dbtools/dbcompare","filePath":"/Users/benoitmarsot/dealerfx/dev/dbtools/java/dbtools-base/dbcompare/src/main/java/dev/platform5/dbtools/dbcompare/Recovery.java","purpose":"assistants", "projectid": 1}'::json)
--
DROP PROCEDURE IF EXISTS core.createoaifile;
CREATE OR REPLACE PROCEDURE core.createoaifile(
    jsonFile json, 
    prid int
) AS $$
DECLARE
    fid int;
    v_file_name varchar(255);
BEGIN
    -- check if the file is already registered
    IF EXISTS(SELECT 1 FROM core.oaifile f WHERE f.oai_f_id = jsonFile->>'fileId') THEN 
        RAISE EXCEPTION 'File with OpenAI file_id % already exists.', jsonFile->>'fileId'
            USING HINT = 'Use a different file ID.';
    END IF;
    
    -- create the file record
    INSERT INTO core.oaifile (
        prid, oai_f_id, file_name, rootdir, filepath, purpose, linecount
    ) VALUES (
        prid,
        jsonFile->>'fileId',
        jsonFile->>'fileName',
        jsonFile->>'rootdir',
        jsonFile->>'filePath',
        jsonFile->>'purpose',
        (jsonFile->>'linecount')::int
    ) RETURNING 
        oaifile.fid,
        oaifile.file_name
    INTO 
        fid,
        v_file_name;
        
    RAISE NOTICE 'Created file: % name: %', fid, v_file_name;
END;
$$ LANGUAGE plpgsql;