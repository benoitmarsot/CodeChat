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
drop procedure core.createuser;
create or replace procedure core.createuser(
    jsonUser json,
    inout userid int default null
) as $$
declare
    username varchar(50);
    useremail varchar(100);
	userrole varchar(20);
begin
    --check if the user is already registered
    if exists(select 1 from core.user u where u.email=jsonUser->>'email') then 
        raise exception 'User with email % already exist.', jsonUser->>'email'
            using hint = 'Please login in instead.';
    end if;
    -- create the user
    insert into core.user (
    	name,email,password,role
    ) values (
        jsonUser->>'name',jsonUser->>'email',jsonUser->>'password',jsonUser->>'role'
    ) returning id,name,email into userid,username,useremail;
    raise notice 'Created user: % name: % with email: % and role: %', userid, username, useremail, userrole;
end
$$
language plpgsql;

-- Create a OaiFile
-- Example:
-- call core.oaifile(
--     '{OaiFile fields and values}'::json
-- )
drop procedure core.createoaifile;
create or replace procedure core.createoaifile(
    jsonFile json
) as $$
begin
    -- check if the file is already registered
    if exists(select 1 from core.oaifile f where f.fileid=jsonFile->>'fileId') then 
        raise exception 'File with fileId % already exist.', jsonFile->>'fileId'
    end if;
    -- insert the file
    insert into core.file (
    	fileId,filename,rootdir,filepath,purpose
    ) values (
        jsonFile->>'fileId',jsonFile->>'fileName',jsonFile->>'rootdir',jsonFile->>'filePath',jsonFile->>'purpose'
    ) returning id,name,email into userid,username,useremail;
    raise notice 'Created file: % name: % for %.', jsonFile->>'fileId', jsonFile->>'fileName', jsonFile->>'purpose'; 
end
$$
language plpgsql;