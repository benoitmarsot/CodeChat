-- Indexes should use IF NOT EXISTS to ensure idempotency
CREATE INDEX IF NOT EXISTS user_email ON core.user(email);

-- For constraints, we should check if they exist before adding
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'email_unique') THEN
        ALTER TABLE core.user ADD CONSTRAINT email_unique UNIQUE (email);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'projectid_filepath_unique') THEN
        ALTER TABLE core.oaifile ADD CONSTRAINT projectid_filepath_unique UNIQUE (projectid, filepath);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_oaifile_projectid ON core.oaifile(projectid);
CREATE INDEX IF NOT EXISTS idx_vectorstore_oaifile_vsid ON core.vectorstore_oaifile(vsid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_assistant_oai_aid ON core.assistant(oai_aid);
CREATE INDEX IF NOT EXISTS idx_assistant_projectid ON core.assistant(projectid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_thread_oai_threadid ON core.thread(oai_threadid);
CREATE INDEX IF NOT EXISTS idx_thread_did ON core.thread(did);
CREATE INDEX IF NOT EXISTS idx_message_did ON core.message(did);
CREATE INDEX IF NOT EXISTS idx_message_authorid ON core.message(authorid);
CREATE INDEX IF NOT EXISTS idx_project_authorid ON core.project(authorid);
CREATE INDEX IF NOT EXISTS idx_discussion_projectid ON core.discussion(projectid);
CREATE INDEX IF NOT EXISTS idx_sharedproject_userid ON core.sharedproject(userid);
CREATE INDEX IF NOT EXISTS idx_sharedproject_projectid ON core.sharedproject(projectid);
