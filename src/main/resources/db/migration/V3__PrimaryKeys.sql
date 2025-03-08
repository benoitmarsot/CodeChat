DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_pkey') THEN
        ALTER TABLE core.user ADD CONSTRAINT user_pkey PRIMARY KEY (userid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'oaifile_pkey') THEN
        ALTER TABLE core.oaifile ADD CONSTRAINT oaifile_pkey PRIMARY KEY (fid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vectorstore_pkey') THEN
        ALTER TABLE core.vectorstore ADD CONSTRAINT vectorstore_pkey PRIMARY KEY (vsid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'vectorstore_oaifile_pkey') THEN
        ALTER TABLE core.vectorstore_oaifile ADD CONSTRAINT vectorstore_oaifile_pkey PRIMARY KEY (vsid, fid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'assistant_pkey') THEN
        ALTER TABLE core.assistant ADD CONSTRAINT assistant_pkey PRIMARY KEY (aid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'thread_pkey') THEN
        ALTER TABLE core.thread ADD CONSTRAINT thread_pkey PRIMARY KEY (threadid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'message_pkey') THEN
        ALTER TABLE core.message ADD CONSTRAINT message_pkey PRIMARY KEY (msgid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'project_pkey') THEN
        ALTER TABLE core.project ADD CONSTRAINT project_pkey PRIMARY KEY (projectid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'discussion_pkey') THEN
        ALTER TABLE core.discussion ADD CONSTRAINT discussion_pkey PRIMARY KEY (did);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'sharedproject_pkey') THEN
        ALTER TABLE core.sharedproject ADD CONSTRAINT sharedproject_pkey PRIMARY KEY (projectid, userid);
    END IF;
END $$;