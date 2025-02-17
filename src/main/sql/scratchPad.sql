-- select * from core.user;
-- select * from core.oaifile;
-- select * from core.vectorstore;
-- select * from core.vectorstore_oaifile;

INSERT INTO core.vectorstore (
    oai_vs_id, vs_name, vs_desc, dayskeep
) VALUES (
    'vs1', 'my tes vs', 'description', 30
);

-- select * from core.vectorstore;
select * from core.vectorstore;
