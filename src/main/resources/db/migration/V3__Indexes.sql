SET search_path TO ${schema};

-- Indexes should use IF NOT EXISTS to ensure idempotency
CREATE INDEX IF NOT EXISTS users_email ON users(email);

-- For constraints, we should check if they exist before adding
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'email_unique') THEN
        ALTER TABLE users ADD CONSTRAINT email_unique UNIQUE (email);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'prid_filepath_unique') THEN
        ALTER TABLE oaifile ADD CONSTRAINT prid_filepath_unique UNIQUE (prid, filepath);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_oaifile_prid ON oaifile(prid);
CREATE INDEX IF NOT EXISTS idx_vectorstore_oaifile_vsid ON vectorstore_oaifile(vsid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_assistant_oai_aid ON assistant(oai_aid);
CREATE INDEX IF NOT EXISTS idx_assistant_projectid ON assistant(projectid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_thread_oai_threadid ON thread(oai_threadid);
CREATE INDEX IF NOT EXISTS idx_thread_did ON thread(did);
CREATE INDEX IF NOT EXISTS idx_message_did ON message(did);
CREATE INDEX IF NOT EXISTS idx_message_authorid ON message(authorid);
CREATE INDEX IF NOT EXISTS idx_project_authorid ON project(authorid);
CREATE INDEX IF NOT EXISTS idx_discussion_projectid ON discussion(projectid);
CREATE INDEX IF NOT EXISTS idx_sharedproject_userid ON sharedproject(userid);
CREATE INDEX IF NOT EXISTS idx_sharedproject_projectid ON sharedproject(projectid);
CREATE INDEX IF NOT EXISTS idx_vectorestore_projectid ON vectorstore(projectid);
CREATE INDEX IF NOT EXISTS idx_usersecret_userid ON usersecret(userid);
CREATE INDEX IF NOT EXISTS idx_projectresource_projectid ON projectresource(projectid);


