CREATE OR REPLACE procedure core.sp_CreatePrimaryKeys()
language plpgsql
as $$
declare
-- variable declaration
begin
    raise notice 'Create primary keys:';

    -- Check and create primary key for core.user
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'user' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.user (userid).';
        ALTER TABLE core.user ADD PRIMARY KEY (userid);
    end if;

    -- Check and create primary key for core.oaifile
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'oaifile' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.oaifile (fid).';
        ALTER TABLE core.oaifile ADD PRIMARY KEY (fid);
    end if;

    -- Check and create primary key for core.vectorstore
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'vectorstore' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.vectorstore (vsid).';
        ALTER TABLE core.vectorstore ADD PRIMARY KEY (vsid);
    end if;

    -- Check and create primary key for core.vectorstore_oaifile
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'vectorstore_oaifile' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.vectorstore_oaifile (vsid, fileid).';
        ALTER TABLE core.vectorstore_oaifile ADD PRIMARY KEY (vsid, fid);
    end if;

    -- Check and create primary key for core.assistant
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'assistant' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.assistant (aid).';
        ALTER TABLE core.assistant ADD PRIMARY KEY (aid);
    end if;

    -- Check and create primary key for core.thread
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'thread' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.thread (threadid).';
        ALTER TABLE core.thread ADD PRIMARY KEY (threadid);
    end if;

    -- Check and create primary key for core.message
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'message' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.message (msgid).';
        ALTER TABLE core.message ADD PRIMARY KEY (msgid);
    end if;

    -- Check and create primary key for core.project
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'project' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.project (projectid).';
        ALTER TABLE core.project ADD PRIMARY KEY (projectid);
    end if;

    -- Check and create primary key for core.discussion
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'discussion' 
            and constraint_schema = 'core' 
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.discussion (did).';
        ALTER TABLE core.discussion ADD PRIMARY KEY (did);
    end if;

    -- Check and create primary key for core.sharedproject
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'sharedproject'
            and constraint_schema = 'core'
            and constraint_type = 'PRIMARY KEY'
    ) then
        raise notice 'Create primary key for core.sharedproject (projectid, userid).';
        ALTER TABLE core.sharedproject ADD PRIMARY KEY (projectid, userid);
    end if;

end; $$;

call core.sp_CreatePrimaryKeys();

drop procedure core.sp_CreatePrimaryKeys;