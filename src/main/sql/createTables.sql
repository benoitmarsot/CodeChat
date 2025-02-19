create table if not exists core.user (
    userid serial,
    name varchar(50) not null,
    email varchar(100) not null,
    password varchar(100),
    role varchar(20) default 'user'
);

create table if not exists core.oaifile (
    fid serial,
    userid int not null,
    oai_f_id varchar(30) not null,
    file_name varchar(255) not null,
    rootdir varchar(1024) not null,
    filepath varchar(1024),
    purpose varchar(20) default 'assistants',
    linecount int not null
);

create table if not exists core.vectorstore (
    vsid serial,
    oai_vs_id text not null,
    vs_name text not null,
    vs_desc text null,
    created timestamp not null default now(),
    dayskeep int null,
    type varchar(20) not null --: code, markup, config, full
);

create table if not exists core.vectorstore_oaifile (
    vsid int not null,
    fid int not null
);

create table if not exists core.assistant (
    aid serial,
    oai_aid varchar(30) not null,
    name varchar(256) not null,
    decsription varchar(512) null,
    codevsid int not null,
    markupvsid int not null,
    configvsid int not null,
    fullvsid int not null -- vector store that contains the sum of codevsid, markupvsid and configvsid 
);

create table if not exists core.thread (
    threadid serial,
    oai_threadid varchar(30) not null,
    vsid int null,
    did int not null,
    type varchar(20) not null -- code, markup, config, full
);

-- the same message in different theads will be stored in the same table,
-- - the internal msgid will be used to identify the message
-- - the openai msgid is diffrent for each thread and is not keeped in the database
-- the msgid of the db is kept in the openai metadata of the message
create table if not exists core.message (
    msgid serial,
    did int not null, -- discussion id
    role varchar(20) not null, -- system, user or assistant
    authorid int not null,
    message text not null
);

create table if not exists core.project (
    projectid int not null,
    name varchar(256) not null,
    description varchar(512) null,
    authorid int not null,
    aid int not null
);

create table if not exists core.discussion (
    did serial,
    projectid int not null,
    name varchar(256) null,
    description varchar(512) null
);

create table if not exists core.sharedproject (
    projectid int not null,
    userid int not null
);