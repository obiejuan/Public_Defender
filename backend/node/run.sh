# copy postgresql config files
cp pg_hba.conf /etc/postgresql/9.5/main/pg_hba.conf
chown postgres /etc/postgresql/9.5/main/pg_hba.conf
cp postgresql.conf /etc/postgresql/9.5/main/postgresql.conf
chown postgres /etc/postgresql/9.5/main/postgresql.conf

# start postgresql server
service postgresql start

# wait 5s to get around slow postgresql startup on slower systems
sleep 5s

# move and import database schema
cp database.initial /var/lib/postgresql/.
chown postgres /var/lib/postgresql/database.initial
su -l postgres -c "psql -f /var/lib/postgresql/database.initial"

# install node modules
npm install

# run node with auto-reloader
node_modules/nodemon/bin/nodemon.js app.js
