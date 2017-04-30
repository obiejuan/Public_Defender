service postgresql start
useradd -p "papAq5PwY/QQM" -s /bin/bash node
cp database.initial /var/lib/postgresql/.
chown postgres /var/lib/postgresql/database.initial
su -l postgres -c "psql -f /var/lib/postgresql/database.initial"
node_modules/nodemon/bin/nodemon.js app.js
