create or replace procedure sp_CreateIndexes()
language plpgsql
as $$
declare
-- variable declaration
begin
    raise notice 'Creating unique constraints:';
    -- if not exists (select constraint_name from information_schema.table_constraints where table_name = 'rwuser' and constraint_name = 'oktaclient_unique') then
    --     raise notice 'Creating rwuser unique index oktaclient_unique...';
    --     alter table rwuser add constraint OktaClient_unique UNIQUE (oktaclientid);
    -- end if;
end; $$;

call public.sp_CreateIndexes();
drop procedure public.sp_CreateIndexes;