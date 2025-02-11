CREATE TABLE IF NOT exists core.user (
    id serial,
    name varchar(50) not null,
    email varchar(100) not null,
    password varchar(100),
    role varchar(20) default 'USER'
);

CREATE TABLE IF NOT exists core.OaiFile (
    fileid varchar(30) not null,
    filename varchar(255) not null,
    rootdir varchar(1024) not null,
    filepath varchar(1024),
    purpose varchar(20) default 'assistants'
);
