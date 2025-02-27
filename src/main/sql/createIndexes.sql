create or replace procedure sp_CreateIndexes()
language plpgsql
as $$
declare
-- variable declaration
begin
    raise notice 'Creating unique constraints:';

    -- Check and create index for core.user email
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'user' 
            and schemaname = 'core' 
            and indexname = 'user_email'
    ) then
        raise notice 'Creating core.user index user_email...';
        create index user_email on core.user(email);
    end if;

    -- Check and create unique constraint for core.user email
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'user' 
            and constraint_schema = 'core' 
            and constraint_name = 'email_unique'
    ) then
        raise notice 'Creating core.user unique index email_unique...';
        alter table core.user add constraint email_unique UNIQUE (email);
    end if;

    -- Check and create unique constraint for core.oaifile projectid and filepath
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'oaifile' 
            and constraint_schema = 'core' 
            and constraint_name = 'projectid_filepath_unique'
    ) then
        raise notice 'Creating core.oaifile unique index projectid_filepath_unique...';
        alter table core.oaifile add constraint projectid_filepath_unique UNIQUE (projectid, filepath);
    end if;

    -- Check and create index for core.oaifile projectid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'oaifile' 
            and schemaname = 'core' 
            and indexname = 'idx_oaifile_projectid'
    ) then
        raise notice 'Creating core.oaifile index idx_oaifile_projectid...';
        create index idx_oaifile_projectid on core.oaifile(projectid);
    end if;

    -- Check and create index for core.vectorstore_oaifile vsid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'vectorstore_oaifile' 
            and schemaname = 'core' 
            and indexname = 'idx_vectorstore_oaifile_vsid'
    ) then
        raise notice 'Creating core.vectorstore_oaifile index idx_vectorstore_oaifile_vsid...';
        create index idx_vectorstore_oaifile_vsid on core.vectorstore_oaifile(vsid);
    end if;

    -- Check and create unique index for core.assistant oai_aid
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

    -- Check and create index for core.assistant projectid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'assistant' 
            and schemaname = 'core' 
            and indexname = 'idx_assistant_projectid'
    ) then
        raise notice 'Creating core.assistant index idx_assistant_projectid...';
        create index idx_assistant_projectid on core.assistant(projectid);
    end if;

    -- Check and create unique index for core.thread oai_threadid
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

    -- Check and create index for core.thread did
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

    -- Check and create index for core.message did
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

    -- Check and create index for core.message authorid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'message' 
            and schemaname = 'core' 
            and indexname = 'idx_message_authorid'
    ) then
        raise notice 'Creating core.message index idx_message_authorid...';
        create index idx_message_authorid on core.message(authorid);
    end if;

    -- Check and create index for core.project authorid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'project' 
            and schemaname = 'core' 
            and indexname = 'idx_project_authorid'
    ) then
        raise notice 'Creating core.project index idx_project_authorid...';
        create index idx_project_authorid on core.project(authorid);
    end if;

    -- Check and create index for core.discussion projectid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'discussion' 
            and schemaname = 'core' 
            and indexname = 'idx_discussion_projectid'
    ) then
        raise notice 'Creating core.discussion index idx_discussion_projectid...';
        create index idx_discussion_projectid on core.discussion(projectid);
    end if;

    -- Check and create index for core.sharedproject userid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'sharedproject' 
            and schemaname = 'core' 
            and indexname = 'idx_sharedproject_userid'
    ) then
        raise notice 'Creating core.sharedproject index idx_sharedproject_userid...';
        create index idx_sharedproject_userid on core.sharedproject(userid);
    end if;

    -- Check and create index for core.sharedproject projectid
    if not exists (
        select indexname 
        from pg_indexes 
        where tablename = 'sharedproject' 
            and schemaname = 'core' 
            and indexname = 'idx_sharedproject_projectid'
    ) then
        raise notice 'Creating core.sharedproject index idx_sharedproject_projectid...';
        create index idx_sharedproject_projectid on core.sharedproject(projectid);
    end if;

end; $$;

call public.sp_CreateIndexes();
drop procedure public.sp_CreateIndexes;