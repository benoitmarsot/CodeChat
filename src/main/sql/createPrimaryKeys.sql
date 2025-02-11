CREATE OR REPLACE procedure core.sp_CreatePrimaryKeys()
language plpgsql
as $$
declare
-- variable declaration
begin
	raise notice 'Create primary keys:';
	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'user' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.user (useid).';
		ALTER TABLE core.user ADD PRIMARY KEY (id);
	end if;
	if not exists (select constraint_name from information_schema.table_constraints where table_name = 'oaifile' and constraint_schema='core' and constraint_type = 'PRIMARY KEY') then
		raise notice 'Create primary key for core.oaifile (fileid).';
		ALTER TABLE core.oaifile ADD PRIMARY KEY (fileid);
	end if;
end; $$;

call core.sp_CreatePrimaryKeys();

drop procedure core.sp_CreatePrimaryKeys;