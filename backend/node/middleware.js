var db = require('./db.js').connection;
var sql = require('./sql.js');
var users = sql.users;

/**
 * Google backend client verification
 */
var CLIENT_ID = "232250430081-g10nohsivb1mgbvdb718628ioicqs2em.apps.googleusercontent.com";
var GoogleAuth = require('google-auth-library');
var auth = new GoogleAuth;

var check_login = (req, res, next) => {
	user_id_token = req.headers['auth-key'];
	if (user_id_token) { //if they have token in headers
		var client = new auth.OAuth2(CLIENT_ID, '', '');
		client.verifyIdToken(user_id_token, CLIENT_ID, (e, login) => {
			if (e) { // was there an error?
				console.log(e);
				res.status(403)
				.json({
					status: 'Forbidden',
					msg: 'Invalid access token provided.',
					error: e.toString()
				});
				return;
			}
			else { // no error, check user id 
				var payload = login.getPayload();
				var q_usr_create = {
					g_email: payload['email'],
					google_id: payload['sub'],
					f_name: payload['given_name'],
					l_name: payload['family_name']
				}
			   /************************************************************
				*	Check if user even exists in the database. If there isn't
				*	any elements returned (no user exists) then we will create
				*	one using the same exact information.
				*	
				* 		SQL: 'select_user' --> check_usr_exists.sql
				*		Var: q_usr_create
				***********************************************************/
				db.any(users.get_by_google_id, q_usr_create)
                .then( (response_db) => {
                    console.log(response_db.length.valueOf())
                    if ( response_db.length.valueOf() < 1 ) {
                        // create the user
                        console.log("Creating user...");
                        console.log(q_usr_create);
                        db.one(users.create , q_usr_create, c => c)
                            .then( (c) => {
                                console.log(`New user: ${c}.`);
                                next();
                            }).catch( (err) => {
                                console.error(err);
                                next(err);
                            });
                    }
                    else { 
                        console.log(response_db[0]) 
                        next();
                    } //else
                }); // 1st then
			} // else
		});
	}
	else {
		res.status(403)
				.json({
					status: 'Forbidden',
					msg: 'No access token provided.'
				});
		return;
	}
}

/* Payton's middleware logging function */
var do_log = (req, res, next) => {
	var ip_regex = new RegExp('((?:[0-9]{1,3}\.?){4})');
	var r_method = req.method;
	var r_path = req.path;
	var r_ipaddr  = req.ip.match(ip_regex)[0];
	var date = new Date();
	var curr_date = date.toLocaleString();

	/*
	 * Log valuable data about the incident 
	 *   Post or Get request
	 *   Eventually username
	 * Write a console logging (middleware) function.
	 * 
	 */
	console.log(`(${curr_date}) from [${r_ipaddr}]: [${r_method}] --> ${r_path}`);
	next();
}

module.exports = { 
                    logging: do_log,
                    login: check_login
                 }
// check_login;