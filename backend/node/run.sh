cp pg_hba.conf /etc/postgresql/9.5/main/pg_hba.conf
chown postgres /etc/postgresql/9.5/main/pg_hba.conf

cp postgresql.conf /etc/postgresql/9.5/main/postgresql.conf
chown postgres /etc/postgresql/9.5/main/postgresql.conf

service postgresql start

cp database.initial /var/lib/postgresql/.
chown postgres /var/lib/postgresql/database.initial
su -l postgres -c "psql -f /var/lib/postgresql/database.initial"

node_modules/nodemon/bin/nodemon.js app.js
