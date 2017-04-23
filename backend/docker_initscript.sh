apt-get update
apt-get install -y curl
curl -sL https://deb.nodesource.com/setup_7.x | bash -
apt-get install -y nodejs postgresql-9.5
service postgresql start
cp database.initial /var/lib/postgresql/.
chown postgres /var/lib/postgresql/database.initial
su -l postgres -c "psql -f /var/lib/postgresql/database.initial"
npm install


