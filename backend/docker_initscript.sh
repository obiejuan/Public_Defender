apt-get update
apt-get install -y curl
curl -sL https://deb.nodesource.com/setup_7.x | bash -
apt-get install -y nodejs postgresql-9.5
systemctl enable postgresql
cp database.initial /var/lib/postgresql/.
cp pg_hba.conf /etc/postgresql/9.5/main/pg_hba.conf
cp postgresql.conf /etc/postgresql/9.5/main/postgresql.conf
chown postgres /var/lib/postgresql/database.initial
su -l postgres -c "psql -f /var/lib/postgresql/database.initial"
service postgresql start
npm install



