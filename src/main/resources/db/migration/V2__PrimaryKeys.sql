SET search_path TO ${schema};

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'users_pkey') THEN
        ALTER TABLE users ADD CONSTRAINT user_pkey PRIMARY KEY (userid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'oaifile_pkey') THEN
        ALTER TABLE oaifile ADD CONSTRAINT oaifile_pkey PRIMARY KEY (fid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vectorstore_pkey') THEN
        ALTER TABLE vectorstore ADD CONSTRAINT vectorstore_pkey PRIMARY KEY (vsid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vectorstore_oaifile_pkey') THEN
        ALTER TABLE vectorstore_oaifile ADD CONSTRAINT vectorstore_oaifile_pkey PRIMARY KEY (vsid, fid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'assistant_pkey') THEN
        ALTER TABLE assistant ADD CONSTRAINT assistant_pkey PRIMARY KEY (aid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'thread_pkey') THEN
        ALTER TABLE thread ADD CONSTRAINT thread_pkey PRIMARY KEY (threadid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'message_pkey') THEN
        ALTER TABLE message ADD CONSTRAINT message_pkey PRIMARY KEY (msgid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'project_pkey') THEN
        ALTER TABLE project ADD CONSTRAINT project_pkey PRIMARY KEY (projectid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'discussion_pkey') THEN
        ALTER TABLE discussion ADD CONSTRAINT discussion_pkey PRIMARY KEY (did);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sharedproject_pkey') THEN
        ALTER TABLE sharedproject ADD CONSTRAINT sharedproject_pkey PRIMARY KEY (projectid, userid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'usersecret_pkey') THEN
        ALTER TABLE usersecret ADD CONSTRAINT usersecret_pkey PRIMARY KEY (userid,prid,label);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'projectressource_pkey') THEN
        ALTER TABLE projectressource ADD CONSTRAINT projectressource_pkey PRIMARY KEY (prid);
    END IF;
END $$;