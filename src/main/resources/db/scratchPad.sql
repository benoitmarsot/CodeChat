-- todo
alter table core.socialuser
    alter column email drop not null 
alter table projectresource
    add column restype varchar(20) null -- git, file, zip, slack, web, etc, ...
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
