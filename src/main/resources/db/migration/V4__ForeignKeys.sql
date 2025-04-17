SET search_path TO ${schema};

DO $$
BEGIN
    raise notice 'Create Foreign keys:';

    -- Foreign key for oaifile(prid) -> projectresource(prid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'oaifile'
            and constraint_schema = 'core'
            and constraint_name = 'oaifile_fk_projectresource'
    ) then
        raise notice 'Creating oaifile_fk_projectresource...';
        alter table oaifile
            add constraint oaifile_fk_projectresource
            foreign key (prid) references projectresource(prid)
            on delete cascade;
    end if;

    -- Foreign key for vectorstore(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'vectorstore'
            and constraint_schema = 'core'
            and constraint_name = 'vectorstore_fk_project'
    ) then
        raise notice 'Creating vectorstore_fk_project...';
        alter table vectorstore 
            add constraint vectorstore_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
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
        ALTER TABLE vectorstore_oaifile 
            ADD CONSTRAINT vectorstore_oaifile_fk_vectorstore 
            FOREIGN KEY (vsid) REFERENCES vectorstore(vsid)
            on delete cascade;
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
        alter table vectorstore_oaifile 
            add constraint vectorstore_oaifile_fk_oaifile 
            foreign key (fid) references oaifile(fid)
            on delete cascade;
    end if;

    -- Foreign key for assistant(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_project'
    ) then
        raise notice 'Creating assistant_fk_project...';
        alter table assistant 
            add constraint assistant_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
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
        alter table assistant
            add constraint assistant_fk_codevsid
            foreign key (codevsid) references vectorstore(vsid)
            on delete cascade;
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_markupvsid'
    ) then
        raise notice 'Creating assistant_fk_markupvsid...';
        alter table assistant
            add constraint assistant_fk_markupvsid
            foreign key (markupvsid) references vectorstore(vsid)
            on delete cascade;
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_configvsid'
    ) then
        raise notice 'Creating assistant_fk_configvsid...';
        alter table assistant
            add constraint assistant_fk_configvsid
            foreign key (configvsid) references vectorstore(vsid)
            on delete cascade;
    end if;

    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'assistant'
            and constraint_schema = 'core'
            and constraint_name = 'assistant_fk_fullvsid'
    ) then
        raise notice 'Creating assistant_fk_fullvsid...';
        alter table assistant
            add constraint assistant_fk_fullvsid
            foreign key (fullvsid) references vectorstore(vsid)
            on delete cascade;
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
        alter table thread 
            add constraint thread_fk_vectorstore
            foreign key (vsid) references vectorstore(vsid)
            on delete cascade;
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
        alter table thread 
            add constraint thread_fk_discussion
            foreign key (did) references discussion(did)
            on delete cascade;
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
        alter table message
            add constraint message_fk_discussion
            foreign key (did) references discussion(did)
            on delete cascade;
    end if;

    -- Foreign key for message(authorid) -> users(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'message'
            and constraint_schema = 'core'
            and constraint_name = 'message_fk_users'
    ) then
        raise notice 'Creating message_fk_users...';
        alter table message 
            add constraint message_fk_users
            foreign key (authorid) references users(userid);
    end if;

    -- Foreign key for project(authorid) -> users(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'project'
            and constraint_schema = 'core'
            and constraint_name = 'project_fk_users'
    ) then
        raise notice 'Creating project_fk_users...';
        alter table project
            add constraint project_fk_users
            foreign key (authorid) references users(userid);
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
        alter table discussion
            add constraint discussion_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
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
        alter table sharedproject 
            add constraint sharedproject_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
    end if;

    -- Foreign key for sharedproject(userid) -> users(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'sharedproject'
            and constraint_schema = 'core'
            and constraint_name = 'sharedproject_fk_users'
    ) then
        raise notice 'Creating sharedproject_fk_users...';
        alter table sharedproject
            add constraint sharedproject_fk_users
            foreign key (userid) references users(userid)
            on delete cascade;
    end if;

    -- Foreign key for usersecret(userid) -> users(userid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'usersecret'
            and constraint_schema = 'core'
            and constraint_name = 'usersecret_fk_users'
    ) then
        raise notice 'Creating usersecret_fk_users...';
        alter table usersecret
            add constraint usersecret_fk_users
            foreign key (userid) references users(userid)
            on delete cascade;
    end if;
    -- Foreign key for projectresource(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'projectresource'
            and constraint_schema = 'core'
            and constraint_name = 'projectresource_fk_project'
    ) then
        raise notice 'Creating projectresource_fk_project...';
        alter table projectresource
            add constraint projectresource_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
    end if;
    -- Foreign key for socialuser(prid) -> projectressource(prid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'socialuser'
            and constraint_schema = 'core'
            and constraint_name = 'socialuser_fk_projectresource'
    ) then
        raise notice 'Creating socialuser_fk_projectresource...';
        alter table socialuser
            add constraint socialuser_fk_projectresource
            foreign key (prid) references projectresource(prid)
            on delete cascade;
    end if;
    -- Foreign key for socialchannel(prid) -> projectressource(prid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'socialchannel'
            and constraint_schema = 'core'
            and constraint_name = 'socialchannel_fk_projectresource'
    ) then
        raise notice 'Creating socialchannel_fk_projectresource...';
        alter table socialchannel
            add constraint socialchannel_fk_projectresource
            foreign key (prid) references projectresource(prid)
            on delete cascade;
    end if;
    -- Foreign key for socialassistant(projectid) -> project(projectid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'socialassistant'
            and constraint_schema = 'core'
            and constraint_name = 'socialassistant_fk_project'
    ) then
        raise notice 'Creating socialassistant_fk_project...';
        alter table socialassistant
            add constraint socialassistant_fk_project
            foreign key (projectid) references project(projectid)
            on delete cascade;
    end if;
    -- Foreign key for socialassistant(vsid) -> vectorstore(vsid)
    if not exists (
        select constraint_name
        from information_schema.table_constraints
        where table_name = 'socialassistant'
            and constraint_schema = 'core'
            and constraint_name = 'socialassistant_fk_vectorstore'
    ) then
        raise notice 'Creating socialassistant_fk_vectorstore...';
        alter table socialassistant
            add constraint socialassistant_fk_vectorstore
            foreign key (vsid) references vectorstore(vsid)
            on delete cascade;
    end if;
END $$;
