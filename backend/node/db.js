/** @author Bryan McCoid  bmccoid_at_ucsc.edu */

// Imports and initializations.
var promise = require('bluebird');
var pgp = require('pg-promise')({ promiseLib: promise });

// PostgreSQL server values
var cn = {
	host: 'localhost',
	port: 5432,
	database: 'pdefender',
	user: 'node',
	password: 'password'
};

// Database object
var db = pgp(cn);

/**@exports connection */
module.exports = { connection: db };