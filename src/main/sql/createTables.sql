CREATE TABLE IF NOT exists core.user (
    userid serial,
    name varchar(50) not null,
    email varchar(100) not null,
    password varchar(100),
    role varchar(20) default 'USER'
);

CREATE TABLE IF NOT exists core.oaifile (
    fid serial,
    userid int not null,
    oai_f_id varchar(30) not null,
    file_name varchar(255) not null,
    rootdir varchar(1024) not null,
    filepath varchar(1024),
    purpose varchar(20) default 'assistants',
    linecount int not null
);

CREATE TABLE IF NOT exists core.vectorstore (
    vsid serial,
    oai_vs_id text not null,
    vs_name text not null,
    vs_desc text null,
    created timestamp not null default now(),
    dayskeep int null
);

CREATE TABLE IF NOT EXISTS core.vectorstore_oaifile (
    vsid int not null,
    fid int not null
);