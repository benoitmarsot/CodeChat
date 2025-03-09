SET search_path TO ${schema};

create table if not exists users (
    userid serial,
    name varchar(50) not null,
    email varchar(100) not null,
    password varchar(100),
    role varchar(20) default 'user'
);

create table if not exists oaifile (
    fid serial,
    projectid int not null,
    oai_f_id varchar(30) not null,
    file_name varchar(255) not null,
    rootdir varchar(1024) not null,
    filepath varchar(1024),
    purpose varchar(20) default 'assistants',
    linecount int not null
);

create table if not exists vectorstore (
    vsid serial,
    oai_vs_id text not null,
    vs_name text not null,
    vs_desc text null,
    created timestamp not null default now(),
    dayskeep int null,
    type varchar(20) not null --: code, markup, config, full
);

create table if not exists vectorstore_oaifile (
    vsid int not null,
    fid int not null
);

create table if not exists assistant (
    aid serial,
    oai_aid varchar(30) not null,
    name varchar(256) not null,
    description varchar(512) null,
    projectid int not null,
    codevsid int not null,
    markupvsid int not null,
    configvsid int not null,
    -- vector store that contains the sum of codevsid, markupvsid and configvsid 
    fullvsid int not null 
);

create table if not exists thread (
    threadid serial,
    oai_threadid varchar(31) not null,
    vsid int null,
    did int not null,
    type varchar(20) not null -- code, markup, config, full
);

-- the same message in different theads will be stored in the same table,
-- - the internal msgid will be used to identify the message
-- - the openai msgid is different for each thread and is not keeped in the database
-- the msgid of the db is kept in the openai metadata of the message
create table if not exists message (
    msgid serial,
    did int not null, -- discussion id
    role varchar(20) not null, -- system, user or assistant
    authorid int not null,
    message text not null,
    created timestamp DEFAULT now()
);

create table if not exists project (
    projectid serial,
    name varchar(256) not null,
    description varchar(512) null,
    authorid int not null,
    isdeleted boolean default false
);

create table if not exists discussion (
    did serial,
    projectid int not null,
    name varchar(256) null,
    description varchar(512) null,
    isfavorite boolean default false,
    created timestamp DEFAULT now()
);

create table if not exists sharedproject (
    projectid int not null,
    userid int not null
);