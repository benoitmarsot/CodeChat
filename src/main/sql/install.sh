#!/bin/bash

cat destroy.sql|psql -Ubenoitmarsot -dreactwell
cat createTables.sql|psql -Ubenoitmarsot -dreactwell
cat createPrimaryKeys.sql|psql -Ubenoitmarsot -dreactwell
cat createForeignKeys.sql|psql -Ubenoitmarsot -dreactwell
cat createIndexes.sql|psql -Ubenoitmarsot -dreactwell
cat createStoreProcedures.sql|psql -Ubenoitmarsot -dreactwell
