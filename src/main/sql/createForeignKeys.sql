CREATE OR REPLACE procedure sp_createforeignkeys()
language plpgsql
as $$
declare
-- variable declaration
begin
	raise notice 'Create Foreign keys:';
    -- Foreign key for oaifile(userid) -> user(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'oaifile'
            and constraint_schema = 'core'
            and constraint_name = 'oaifile_fk_user'
    ) then
        raise notice 'Creating oaifile_fk_user...';
        alter table core.oaifile
            add constraint oaifile_fk_user
            foreign key (userid) references core.user(userid);
    end if;
    -- Foreign key for vectorstore_oaifile(vsid) -> vectorstore(vsid)
    IF NOT EXISTS (
        SELECT constraint_name 
        FROM information_schema.table_constraints 
        WHERE table_name = 'vectorstore_oaifile' 
            AND constraint_schema = 'core'
            AND constraint_name = 'vectorstore_oaifile_fk_vectorstore'
    ) THEN
        RAISE NOTICE 'Creating vectorstore_oaifile_fk_vectorstore...';
        ALTER TABLE core.vectorstore_oaifile 
            ADD CONSTRAINT vectorstore_oaifile_fk_vectorstore 
            FOREIGN KEY (vsid) REFERENCES core.vectorstore(vsid);
    END IF;
	-- Foreign key for vectorstore_oaifile(fid) -> oaifile(fid)
    if not exists (
        select constraint_name 
        from information_schema.table_constraints 
        where table_name = 'vectorstore_oaifile' 
            and constraint_schema='core'
            and constraint_name = 'vectorstore_oaifile_fk_oaifile'
    ) then
        raise notice 'Creating vectorstore_oaifile_fk_oaifile...';
        alter table core.vectorstore_oaifile 
			add constraint vectorstore_oaifile_fk_oaifile 
			foreign key (fid) references core.oaifile(fid);
    end if;

    -- Foreign keys for assistant codevsid, markupvsid, configvsid, fullvsid -> vectorstore(vsid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_codevsid'
    ) then
        raise notice 'Creating assistant_fk_codevsid...';
        alter table core.assistant
            add constraint assistant_fk_codevsid
            foreign key (codevsid) references core.vectorstore(vsid);
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_markupvsid'
    ) then
        raise notice 'Creating assistant_fk_markupvsid...';
        alter table core.assistant
            add constraint assistant_fk_markupvsid
            foreign key (markupvsid) references core.vectorstore(vsid);
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_configvsid'
    ) then
        raise notice 'Creating assistant_fk_configvsid...';
        alter table core.assistant
            add constraint assistant_fk_configvsid
            foreign key (configvsid) references core.vectorstore(vsid);
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_fullvsid'
    ) then
        raise notice 'Creating assistant_fk_fullvsid...';
        alter table core.assistant
            add constraint assistant_fk_fullvsid
            foreign key (fullvsid) references core.vectorstore(vsid);
    end if;

	-- Foreign key for thread(vsid) -> vectorstore(vsid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'thread'
            and constraint_schema = 'core'
            and constraint_name = 'thread_fk_vectorstore'
    ) then
        raise notice 'Creating thread_fk_vectorstore...';
        alter table core.thread 
            add constraint thread_fk_vectorstore
            foreign key (vsid) references core.vectorstore(vsid);
    end if;

    -- Foreign key for thread(did) -> discussion(did)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'thread'
            and constraint_schema = 'core'
            and constraint_name = 'thread_fk_discussion'
    ) then
        raise notice 'Creating thread_fk_discussion...';
        alter table core.thread 
            add constraint thread_fk_discussion
            foreign key (did) references core.discussion(did);
    end if;

    -- Foreign key for message(did) -> discussion(did)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'message'
            and constraint_schema = 'core'
            and constraint_name = 'message_fk_discussion'
    ) then
        raise notice 'Creating message_fk_discussion...';
        alter table core.message
            add constraint message_fk_discussion
            foreign key (did) references core.discussion(did);
    end if;

    -- Foreign key for message(authorid) -> user(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'message'
            and constraint_schema = 'core'
            and constraint_name = 'message_fk_user'
    ) then
        raise notice 'Creating message_fk_user...';
        alter table core.message 
            add constraint message_fk_user
            foreign key (authorid) references core.user(userid);
    end if;

	-- Foreign key for project(authorid) -> user(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'project'
            and constraint_schema = 'core'
            and constraint_name = 'project_fk_user'
    ) then
        raise notice 'Creating project_fk_user...';
        alter table core.project
            add constraint project_fk_user
            foreign key (authorid) references core.user(userid);
    end if;

    -- Foreign key for project(aid) -> assistant(aid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'project'
            and constraint_schema = 'core'
            and constraint_name = 'project_fk_assistant'
    ) then
        raise notice 'Creating project_fk_assistant...';
        alter table core.project 
            add constraint project_fk_assistant
            foreign key (aid) references core.assistant(aid);
    end if;

    -- Foreign key for discussion(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'discussion'
            and constraint_schema = 'core'
            and constraint_name = 'discussion_fk_project'
    ) then
        raise notice 'Creating discussion_fk_project...';
        alter table core.discussion
            add constraint discussion_fk_project
            foreign key (projectid) references core.project(projectid);
    end if;

    -- Foreign key for sharedproject(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'sharedproject'
            and constraint_schema = 'core'
            and constraint_name = 'sharedproject_fk_project'
    ) then
        raise notice 'Creating sharedproject_fk_project...';
        alter table core.sharedproject 
            add constraint sharedproject_fk_project
            foreign key (projectid) references core.project(projectid);
    end if;

    -- Foreign key for sharedproject(userid) -> user(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'sharedproject'
            and constraint_schema = 'core'
            and constraint_name = 'sharedproject_fk_user'
    ) then
        raise notice 'Creating sharedproject_fk_user...';
        alter table core.sharedproject
            add constraint sharedproject_fk_user
            foreign key (userid) references core.user(userid);
    end if;
end; $$;

call public.sp_CreateForeignKeys();
drop procedure public.sp_CreateForeignKeys;

