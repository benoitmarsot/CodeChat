-- Functions
-- provider sign in
-- arg {email:un,password:pw}
-- sample: select providersignin('{"email":"provemail@hotmail.com","password":"password"}');
-- create or replace FUNCTION providersignin(json) 
-- RETURNS json AS 
-- $$
--     select json_build_object(
--         'providerId', p.providerid,
--         'rwuserId', p.rwuserid,
--         'firstName', p.firstname,
--         'lastName', p.lastname,
--         'company', p.company,
--         'address', p.address,
--         'city' , p.city,
--         'usState', p.usstate,
--         'zip', p.zip,
--         'email', u.email,
--         'patients', case when max(pa.patientid) is null then '[]' else 
-- 	        json_agg(json_build_object(
-- 	            'patientId',pp.patientid,
-- 	            'firstName',pa.firstname,
-- 	            'lastName',pa.lastname
-- 	        ) ORDER BY pa.firstname, pa.lastname DESC)
-- 	    end
--     )
--     from public.provider p
--         inner join public.rwuser u on p.rwuserid = u.rwuserid
--         left join public.providerpatient pp on p.providerid=pp.providerid
--         left join public.patient pa on pp.patientid=pa.patientid 
--     where u.email=$1->>'email' and u.rwpassword=$1->>'password'
--     group by p.providerid, u.email
--     ;
-- $$
-- language sql;

-- update a patient profile
-- Example:
--  call public.updatepatient(
--     '{"providerId":1,"firstName":"Benoit","lastName":"Marsot","company":"unBumpkin","address":"4135 21ST ST","city":"SAN FRANCISCO","usState":"CA","zip":"94114","password":"test"}'::json
--     ,1
-- )
-- create or replace procedure updatepatient(
--     jsonPatient json,
--     inout pId int default null
-- ) as $$
-- begin
--     -- check if the patient exist
--     if not exists ( select 1 from public.patient where patientid=pId ) then 
--         raise exception 'Patient id % does not exist.', pId;
--     end if;

--     -- update the patient
--     update public.patient set (
--         firstname, lastname, address, city, usstate, zip, referral
--     ) = (
--         jsonPatient->>'firstName',jsonPatient->>'lastName', jsonPatient->>'address',
--         jsonPatient->>'city', jsonPatient->>'usState',jsonPatient->>'zip',jsonPatient->>'referral'
--     ) where patientid=pId;

--     raise notice 'Updated pattient: %', pId;
-- end
-- $$
-- language plpgsql;

-- procedures
-------------

-- Create a user
-- Example:
-- call core.createuser(
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
BEGIN
    -- check if the user is already registered
    IF EXISTS(SELECT 1 FROM core.user u WHERE u.email = jsonUser->>'email') THEN 
        RAISE EXCEPTION 'User with email % already exist.', jsonUser->>'email'
            USING HINT = 'Please login in instead.';
    END IF;
    
    -- create the user
    INSERT INTO core.user (
        name, email, password, role
    ) VALUES (
        jsonUser->>'name',
        jsonUser->>'email',
        jsonUser->>'password',
        jsonUser->>'role'
    ) RETURNING 
        core.user.userid,
        core.user.name,
        core.user.email 
    INTO 
        out_userid,
        v_name,
        v_email;
        
    RAISE NOTICE 'Created user: % name: % with email: % and role: %', out_userid, v_name, v_email, v_role;
END;
$$ LANGUAGE plpgsql;

-- Create a OaiFile
-- Example:
-- call core.createoaifile('{"fileId":"file-G8esRwhimXuLiYXVd5RuXE","fileName":"Recovery.java","rootdir":"/Users/benoitmarsot/dealerfx/dev/dbtools/java/dbtools-base/dbcompare/src/main/java/dev/platform5/dbtools/dbcompare","filePath":"/Users/benoitmarsot/dealerfx/dev/dbtools/java/dbtools-base/dbcompare/src/main/java/dev/platform5/dbtools/dbcompare/Recovery.java","purpose":"assistants"}'::json)
--
DROP PROCEDURE IF EXISTS core.createoaifile;
CREATE OR REPLACE PROCEDURE core.createoaifile(
    jsonFile json,
    userId int
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
        userid, oai_f_id, file_name, rootdir, filepath, purpose, linecount
    ) VALUES (
        userId,
        jsonFile->>'fileId',
        jsonFile->>'fileName',
        jsonFile->>'rootdir',
        jsonFile->>'filePath',
        jsonFile->>'purpose',
        (jsonFile->>'linecount')::int
    ) RETURNING 
        core.oaifile.fid,
        core.oaifile.file_name
    INTO 
        fid,
        v_file_name;
        
    RAISE NOTICE 'Created file: % name: %', fid, v_file_name;
END;
$$ LANGUAGE plpgsql;