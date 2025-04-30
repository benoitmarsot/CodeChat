SET search_path TO public;

-- Indexes should use IF NOT EXISTS to ensure idempotency
CREATE INDEX IF NOT EXISTS users_email ON core.users(email);

-- For constraints, we should check if they exist before adding
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'email_unique') THEN
        ALTER TABLE core.users ADD CONSTRAINT email_unique UNIQUE (email);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'prid_filepath_unique') THEN
        ALTER TABLE core.oaifile ADD CONSTRAINT prid_filepath_unique UNIQUE (prid, filepath);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_oaifile_prid ON core.oaifile(prid);
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
CREATE INDEX IF NOT EXISTS idx_vectorestore_projectid ON core.vectorstore(projectid);
CREATE INDEX IF NOT EXISTS idx_usersecret_userid ON core.usersecret(userid);
CREATE INDEX IF NOT EXISTS idx_projectresource_projectid ON core.projectresource(projectid);
CREATE INDEX IF NOT EXISTS idx_socialuser_prid ON core.socialuser(prid);
CREATE INDEX IF NOT EXISTS idx_socialchannel_prid ON core.socialchannel(prid);
CREATE INDEX IF NOT EXISTS idx_socialassistant_projectid ON core.socialassistant(projectid);
CREATE INDEX IF NOT EXISTS idx_chunk_projectid_chunktype ON core.chunk (projectid, chunktype);
-- For vector search
CREATE INDEX IF NOT EXISTS idx_chunk_embedding_ivfflat
    ON core.chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
-- For full-text search
CREATE INDEX IF NOT EXISTS idx_chunk_projectid_chunktype_content_gin
    ON core.chunk USING gin (to_tsvector('english', content));



