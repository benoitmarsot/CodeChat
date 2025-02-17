CREATE OR REPLACE procedure core.sp_CreatePrimaryKeys()
language plpgsql
as $$
declare
-- variable declaration
begin
	raise notice 'Create primary keys:';
	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'user' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.user (userid).';
		ALTER TABLE core.user ADD PRIMARY KEY (userid);
	end if;

	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'oaifile' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.oaifile (fid).';
		ALTER TABLE core.oaifile ADD PRIMARY KEY (fid);
	end if;
	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'vectorstore' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.vectorstore (vsid).';
		ALTER TABLE core.vectorstore ADD PRIMARY KEY (vsid);
	end if;
	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'vectorstore_oaifile' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.vectorstore_oaifile (vsid, fileid).';
		ALTER TABLE core.vectorstore_oaifile ADD PRIMARY KEY (vsid, fid);
	end if;
	
end; $$;

call core.sp_CreatePrimaryKeys();

drop procedure core.sp_CreatePrimaryKeys;