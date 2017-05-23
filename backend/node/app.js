var express = require('express');
var promise = require('bluebird');
const fs = require('fs');
var bodyParser = require("body-parser");
var pgp = require('pg-promise')({
	promiseLib: promise
});


/**
 * Google backend client verification
 */
var GoogleAuth = require('google-auth-library');
var auth = new GoogleAuth;
var CLIENT_ID = "232250430081-g10nohsivb1mgbvdb718628ioicqs2em.apps.googleusercontent.com";
var test_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjJiMmU0ZDZmMTgyMWFkNGQ3NmQ0NTUzYzk1MWI3NTYyMmFjYjY1MzkifQ.eyJhenAiOiIyMzIyNTA0MzAwODEtdXFxZmQ3dWh0cmdwbGk3dWpoNzZ2dmV0MGszaTRvOTAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIyMzIyNTA0MzAwODEtZzEwbm9oc2l2YjFtZ2J2ZGI3MTg2Mjhpb2ljcXMyZW0uYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDU1OTY1ODg5NDc3NzAyODYwNDAiLCJoZCI6InVjc2MuZWR1IiwiZW1haWwiOiJibWNjb2lkQHVjc2MuZWR1IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbSIsImlhdCI6MTQ5NTIyMDIwNiwiZXhwIjoxNDk1MjIzODA2LCJuYW1lIjoiQnJ5YW4gTWNjb2lkIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS8tampvTkQwYXJ0Sm8vQUFBQUFBQUFBQUkvQUFBQUFBQUFBQUEvQUhhbEdoclFNV3B5Q2hjNUxteTVORjVFeHRpUjFSZkpJdy9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiQnJ5YW4iLCJmYW1pbHlfbmFtZSI6Ik1jY29pZCIsImxvY2FsZSI6ImVuIn0.fig0YY_pohogRen64FlkhqWWQ_jKHh625ULAbjhuhaANpWnpLFMUFDdbMoipjN3JcbyrZOZXLp-SmmpmCQ9N6UbF7GvBIPx1vFj9objksrUoyxQ0jjjapt5AeOOavOrlgAk7IZHixm5V8-dRp2trZ3zVincLlCY769bWwMqRGm0omgQut_z9sTyZLnu_9lEz9S9nPHTwo5OJMctZjTW0ogrktY2_dK1bs-2afjcEo4IX2zOZxsddWpQ14KqQyOtSuPaQq57IoRy0fuC_evPXdp1qJrQd9GAsxFqWP23skS_06yNhs2a1vfNKcxNoJQXfxI3BA_DfW472AqwGXHLVJQ";
// for unique recording identifier generation
const uuid = require('uuid/v4');

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

/**
 * Stream buffers
 */
var test_buff = new Buffer.alloc(64000, 'base64') // 62.5 KB ~4 seconds of buffer for 16Bit 8khz PCM MONO audio
var live_streams = {"1": test_buff}

/**
 * Setup postgres connection and Express.js
 */
var db = pgp(cn);
var app = express()

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
					g_userid: payload['sub'],
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
				db.any(select_user, q_usr_create)
					.then( (response_db) => {
						console.log(response_db.length.valueOf())
						if ( response_db.length.valueOf() < 1 ) {
							// create the user
							console.log("Creating user...");
							console.log(q_usr_create);
							db.one(create_user , q_usr_create, c => c)
								.then( (c) => {
									console.log(`New user: ${c}.`);
									next();
								}).catch( (err) => {
									console.err(err);
									next(err);
								});
						}
						else { 
							console.log(response_db[0]) 
							next();
						}
					});
			}
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

// Run the middleware logger.
app.use(do_log, check_login) //

/**
 *	Routes: Only parsing json automatically where needed.
 */
app.post('/upload/', bodyParser.json(), upload)
app.post('/user/', bodyParser.json())
app.post('/nearby/', bodyParser.json(), nearby)

/**
 *	Helper function for setting up QueryFiles
 *	usage:
 *		var sqlFindUser = sql('./sql/findUser.sql');
 *	param:
 *		file: filename (string)
 */
function sql(file) {
	return new pgp.QueryFile(file, {
		minify: true
	});
}

/**
 *	Setup QueryFiles once at startup
 */
var stopEvent = sql('./sql_queries/pd_event/update_status.sql');
var getNearby = sql('./sql_queries/pd_event/get_nearby.sql');
var init_upload = sql('./sql_queries/pd_event/init_upload.sql');
var new_recording = sql('./sql_queries/pd_recording/new_recording.sql');
var select_user = sql('./sql_queries/pd_user/check_usr_exists.sql');
var create_user = sql('./sql_queries/pd_user/create_usr.sql');

/**
 * Just returns all events, with associated user and recordings
 */
app.get('/', (req, res, next) => { 
	db.any('select * FROM pd_user INNER JOIN pd_event ON pd_event.pd_user_id=pd_user.user_id INNER JOIN pd_recording ON pd_event.event_id=pd_recording.event_id WHERE pd_event.active=false')
		.then( (data) => {
			res.status(200)
				.json({
					status: 'success',
					data: data,
					message: 'All events included.'
				});
		})
		.catch( (err) => { 
			return next(err);
		});
})

/************************************************************************
 * Get's the nearby active=true events for the parameters in #Request.
 * 
 * Uses: 
 * @file get_nearby.sql
 * 
 * JSON keys: 
 * @param {string} current_location
 * @param {int} distance
 ***********************************************************************/
function nearby(req, res, next) {
	var date_now = Date.now();
	var query = {
		current_location: req.body.current_location,
		distance: req.body.distance
	};
	//console.log(query);

	db.any(getNearby, query)
		.then( (data) => {
			console.log(data.length);
			res.status(200)
				.json({
					status: 'success',
					data: data,
				});
		})
		.catch( (err) => {
			console.log(err);
		}).then( (err) => { // error resonses
			res.status(500)
				.json({
					status: 'error',
					msg: err,
				})
		}).catch( (err) => {});
}


/*********************************************************************************************
 * Lists all
 *  
 * Table "public.pd_user"
 *  Column  |          Type          |                      Modifiers                       
 * ---------+------------------------+------------------------------------------------------
 * user_id  | integer                | not null default nextval('pd_user_id_seq'::regclass)
 * auth_key | character varying(255) | 
 * email    | character varying(255) | 
 * 
 *********************************************************************************************/
app.post('/users/', (req, res, next) => {
	db.any('select * from pd_user').then( (data) => {
			res.status(200)
				.json({
					status: 'success',
					data: data
				});
		}).catch( (err) => {
			console.log(err)
			return err;
		}).then( (err) => { // error resonses
			res.status(500)
				.json({
					status: 'error',
					msg: err,
				})
		}).catch( (err) => {console.error(err)} ); //end catch of db.any then 
});

/******************************************************************************************************
 * 	Table "public.pd_event"
 *	   Column   |            Type             |                       Modifiers                       
 *	------------+-----------------------------+-------------------------------------------------------
 *	 event_id   | integer                     | not null default nextval('pd_event_id_seq'::regclass)
 *	 location   | point                       | 
 *	 pd_user_id | integer                     | not null
 *	 active     | boolean                     | not null default true
 *	 event_date | timestamp without time zone | not null
 ******************************************************************************************************
 * Uses:
 * @file init_upload.sql
 * @file new_recording.sql
 * 
 * JSON keys: 
 * @param {int} user
 * @param {string} location
 * 
 * Expressjs:
 * @param {any} req 
 * @param {any} res 
 * @param {any} next 
 * 
 **/
function upload (req, res, next) {
/*	res.status(500).json({
						status: 'error',
						msg: "some error message",
						upload_token: null,
						url: null,
					});*/
	var { current_location, user } = req.body
	var event_id
	date = new Date()
	var unique_token
	
	db.tx( (t) => {
		return t.one(select_user, {g_userid: user}, c => +c.user_id) //google_id --> pd_user_id
			.then( (userid) => {
				console.log(userid);
				unique_token = userid + '_' + uuid()
				record_data = {
					user_id: userid,
					geo: current_location,
					active: true,
					timestamp: date
				};
				console.log(record_data);
				return t.one(init_upload, record_data, c => +c.event_id) //create event --> event_id
			}).then( (id) => {
				console.log(unique_token)
				event_id = id;
				recording_data = {
					event_id: id,
					filename: unique_token
				}
				return t.none(new_recording, recording_data);
			});
		}).then( () => {
			console.log(event_id, unique_token);
			res.status(200)
				.json({
					status: 'success',
					upload_token: unique_token, //token all by itself
					url: `${event_id}/${unique_token}/` //unique upload url
				});
		})
		.catch( (err) => {
			console.error(err);
		}).then( (err) => { 
			res.status(500)
				.json({
					status: 'error',
					msg: err,
					upload_token: null,
					url: null,
				});
		}).catch( () => {});
}

/*****************************************************
 * Table "public.pd_recording"
 *	Column    |         Type           | Modifiers 
 *	----------+------------------------+-----------
 *	event_id  | integer                | not null
 *	filename  | character varying(255) | not null
 *****************************************************
 * Recieves the streaming data from client through a POST request.
 * JSON:
 * @param {int} id #filename
 * @param {int} event #event_id
 * 
 * @param {any} req 
 * @param {any} res 
 * @param {any} next 
 */
function recieve_stream (req, res, next) {
	file_id = req.params.id
	event = req.params.event
	live_streams[event] = null;
	//live_streams[event] = Buffer.alloc(64000, 0, 'base64')
	console.log("Beginning transfer for unique key: " + file_id)

	fileStream = fs.createWriteStream("./data_files/" + file_id + ".pcm")
	req.pipe(fileStream)

	var tick = 0
	var total_bytes = 0;
	var errorMsg = null
	var stop_data = {
		active: false,
		event_id: event
	}
	/**
	 * Called any time data is recieved from the request. Logs how much is transferred
	 * and also keeps track of the number of chunks. 
	 * @param {bytes[]} chunk
	 **/
	req.on('data', (chunk) => {
		console.log(tick + ": " + (chunk.length / 1024).toFixed(2) + " KiB")
		total_bytes = total_bytes + chunk.length
		tick++;

		/**
		 * Testing for live stream implementation
		 */
		//console.log(chunk.toString('base64'))
		if (Buffer.isBuffer(live_streams[event])) {
			var buff2 = new Buffer(chunk.toString('base64'))
			live_streams[event] = Buffer.concat( [ live_streams[event] , buff2 ] );
		}
		else {
			live_streams[event] = new Buffer(chunk.toString('base64'));
		}
		
	})
	/**
	 * Called when the connection / request is ended. 
	 **/
	req.on('end',  () => {
		db.none(stopEvent, stop_data)
			.then(() => {
				console.log("Successful pd_event table update.");
			})
			.catch((err) => {
				console.log("Error updating pd_event table.");
				console.log(err);
			});
		console.log("End: " + (total_bytes / 1024).toFixed(2) + " KiB transfered.")
		console.log("") //newline for prettier output on console
	});
	req.on('error', (err) => {
		errorMsg = err
		console.log("Error: " + err)
	});
	/**
	 * Send response to client. Minimal currently.
	 **/
	res.send({
		"UploadCompleted": true,
		"Error": errorMsg,
	});
}
app.post('/upload/:event/:id/', recieve_stream);

/** 
 * Run the actual server
 **/
/*
io.on('connection', function(){  });
server.listen(3000);
*/
app.listen(3000, () => {
	console.log('Currently defending on port 3000!!')
});
