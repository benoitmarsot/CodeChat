create or replace procedure sp_CreateIndexes()
language plpgsql
as $$
declare
-- variable declaration
begin
    raise notice 'Creating unique constraints:';
    if not exists (select indexname from pg_indexes where tablename = 'user' and schemaname = 'core' and indexname = 'user_email') then
        raise notice 'Creating core.user index user_email...';
        create index user_email on core.user(email);
    end if;
    if not exists (select constraint_name from information_schema.table_constraints where table_name = 'oaifile' and constraint_schema='core' and constraint_name = 'userid_filepath_unique') then
        raise notice 'Creating core.oaifile unique index userid_filepath_unique...';
        alter table core.oaifile add constraint userid_filepath_unique UNIQUE (userid,filepath);
    end if;
    if not exists (select indexname from pg_indexes where tablename = 'oaifile' and schemaname = 'core' and indexname = 'idx_oaifile_oai_f_id') then
        raise notice 'Creating core.oaifile index idx_oaifile_oai_f_id...';
        create index idx_oaifile_oai_f_id on core.oaifile(oai_f_id);
    end if;
    if not exists (select constraint_name from information_schema.table_constraints where table_name = 'user' and constraint_schema='core' and constraint_name = 'email_unique') then
        raise notice 'Creating core.user unique index email_unique...';
        alter table core.user add constraint email_unique UNIQUE (email);
    end if;
    if not exists (select indexname from pg_indexes where tablename = 'vectorstore' and schemaname = 'core' and indexname = 'idx_vectorstore_oai_vs_id') then
        raise notice 'Creating core.vectorstore index idx_vectorstore_vsid...';
        create index idx_vectorstore_oai_vs_id on core.vectorstore(oai_vs_id);
    end if;
    if not exists (select indexname from pg_indexes where tablename = 'vectorstore_oaifile' and schemaname = 'core' and indexname = 'idx_vectorstore_oaifile_vsid') then
        raise notice 'Creating core.vectorstore_oaifile index idx_vectorstore_oaifile_vsid...';
        create index idx_vectorstore_oaifile_vsid on core.vectorstore_oaifile(vsid);
    end if;
    
    -- Add unique index for assistant.oai_aid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'assistant' 
            and schemaname = 'core' 
            and indexname = 'idx_assistant_oai_aid'
    ) then
        raise notice 'Creating unique index idx_assistant_oai_aid on core.assistant(oai_aid)...';
        create unique index idx_assistant_oai_aid on core.assistant(oai_aid);
    end if;

    -- Add unique index for thread.oai_threadid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'thread' 
            and schemaname = 'core' 
            and indexname = 'idx_thread_oai_threadid'
    ) then
        raise notice 'Creating unique index idx_thread_oai_threadid on core.thread(oai_threadid)...';
        create unique index idx_thread_oai_threadid on core.thread(oai_threadid);
    end if;

    -- Add index for message.did
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'message' 
            and schemaname = 'core' 
            and indexname = 'idx_message_did'
    ) then
        raise notice 'Creating index idx_message_did on core.message(did)...';
        create index idx_message_did on core.message(did);
    end if;

    -- Add index for discussion.projectid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'discussion' 
            and schemaname = 'core' 
            and indexname = 'idx_discussion_projectid'
    ) then
        raise notice 'Creating index idx_discussion_projectid on core.discussion(projectid)...';
        create index idx_discussion_projectid on core.discussion(projectid);
    end if;

    -- Add index for thread.did
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'thread' 
            and schemaname = 'core' 
            and indexname = 'idx_thread_did'
    ) then
        raise notice 'Creating index idx_thread_did on core.thread(did)...';
        create index idx_thread_did on core.thread(did);
    end if;
end; $$;

call public.sp_CreateIndexes();
drop procedure public.sp_CreateIndexes;