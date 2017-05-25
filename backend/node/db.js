var promise = require('bluebird');
var pgp = require('pg-promise')({
	promiseLib: promise
});
/**
 * Setup server values
 */
var cn = {
	host: 'localhost',
	port: 5432,
	database: 'pdefender',
	user: 'node',
	password: 'password'
};

var db = pgp(cn);

module.exports = {
    connection: db
}