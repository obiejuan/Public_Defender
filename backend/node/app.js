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
var the_CLIENT_ID = "232250430081-g10nohsivb1mgbvdb718628ioicqs2em.apps.googleusercontent.com";
var test_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjQyMTcxMDE1MzMwZjEwN2FhN2QxYjg1NGJmY2Y1NGE0ZjVhNTA3MDUifQ.eyJhenAiOiIyMzIyNTA0MzAwODEtdXFxZmQ3dWh0cmdwbGk3dWpoNzZ2dmV0MGszaTRvOTAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIyMzIyNTA0MzAwODEtZzEwbm9oc2l2YjFtZ2J2ZGI3MTg2Mjhpb2ljcXMyZW0uYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDU1OTY1ODg5NDc3NzAyODYwNDAiLCJoZCI6InVjc2MuZWR1IiwiZW1haWwiOiJibWNjb2lkQHVjc2MuZWR1IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbSIsImlhdCI6MTQ5NTE3MTU5MSwiZXhwIjoxNDk1MTc1MTkxLCJuYW1lIjoiQnJ5YW4gTWNjb2lkIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS8tampvTkQwYXJ0Sm8vQUFBQUFBQUFBQUkvQUFBQUFBQUFBQUEvQUhhbEdoclFNV3B5Q2hjNUxteTVORjVFeHRpUjFSZkpJdy9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiQnJ5YW4iLCJmYW1pbHlfbmFtZSI6Ik1jY29pZCIsImxvY2FsZSI6ImVuIn0.IReSTU_pji6Copdv2JM8mpWP-5iQwt2-i0j04msOobG1f0bnBg_JNOvEbAt6UxuTnp0gOj1iSzxeNM6AdJt2OT6ZEx0lyc76EjfIrcLsuemKotNzlaMgUiyf8EMg3VXm7T9rYKe4hoZIclHhwcQ8-gSSNtcwZT34nhtOEX5CMdN3FyFIZxNn6eSrME3f2v58mxVp8C9L8rvo1LKwWKmLiQiUqxtNsllX_1jUvHTTLeIE_Yi51h5tzJeMslYBdvcIJebYoErPlyepRyFKIpmfXcWY75VWZM_6sQhhFWb9Uj-735T9GchB8Xcn8Sq6-ODFo7JpNK0UPScFgiAxf_Ne1Q";

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

/* Payton's middleware logging function */
var do_log = function (req, res, next) {
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
	console.log(`${curr_date} [${r_method}] request in <${r_path}> by ${r_ipaddr}`);
	next()
}

// Run the middleware logger.
app.use(do_log)

/**
 *	Routes: Only parsing json automatically where needed.
 */
app.post('/upload/', bodyParser.json())
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

/**
 * Just returns all events, with associated user and recordings
 */
app.get('/', function (req, res, next) { 
	db.any('select * FROM pd_user INNER JOIN pd_event ON pd_event.pd_user_id=pd_user.user_id INNER JOIN pd_recording ON pd_event.event_id=pd_recording.event_id WHERE pd_event.active=false')
		.then(function (data) {
			res.status(200)
				.json({
					status: 'success',
					data: data,
					message: 'All events included.'
				});
		})
		.catch(function (err) { 
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
	db.any(getNearby, query)
		.then(function (data) {
			res.status(200)
				.json({
					status: 'success',
					data: data,
				});
		})
		.catch(function (err) {
			return next(err);
		}).then(function (err) { // error resonses
			res.status(500)
				.json({
					status: 'error',
					msg: err,
				})
		}).catch(function (err) {});
}

function lg_in (e, login) {
	var payload = login.getPayload();
	var userid = payload['sub'];
	console.log(e);
}

/*********************************************************************************************
 * User creation:
 *  
 * Table "public.pd_user"
 *  Column  |          Type          |                      Modifiers                       
 * ---------+------------------------+------------------------------------------------------
 * user_id  | integer                | not null default nextval('pd_user_id_seq'::regclass)
 * auth_key | character varying(255) | 
 * email    | character varying(255) | 
 * 
 *********************************************************************************************/
app.post('/user/', function (req, res, next) {
	user_id_token = req.body.user_id_token;
	/**
	 * TODO:
	 * 1.) check if key is valid
	 * 2.) parse details
	 * 3.) check if user exists
	 * 4.) if not create user
	 * 
	 */
	

	var client = new auth.OAuth2(the_CLIENT_ID, '', '');
	client.verifyIdToken(user_id_token, the_CLIENT_ID, function(e, login) {
		var payload = login.getPayload();
		var userid = payload['sub'];
		console.log(e);
		console.log(userid);
	});


	user_promise = db.query('INSERT INTO pd_user (auth_key, email) VALUES ($1, $2)', [user.auth_key, user.email]).then(function (response_db) {
		db.any('select * from pd_user').then(function (data) {
			res.status(200)
				.json({
					status: 'success',
					data: data
				});
			console.log(user_promise);
		}).catch(function (err) {
			console.log(err)
			return err;
		}).then(function (err) { // error resonses
			res.status(500)
				.json({
					status: 'error',
					msg: err,
				})
		}); //end catch of db.any then 

	}).catch(function (err) {
		console.log(err)
		return err;
	}).then(function (err) { // error resonses
		res.status(500)
			.json({
				status: 'error',
				msg: err,
			})
	});
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
	var { location, user } = req.body
	var event_id
	unique_token = user + '_' + uuid()
	date = new Date()

	var record_data = {
		user_id: user,
		geo: location,
		active: true,
		timestamp: date
	};
	db.tx(function (t) {
			return t.one(init_upload, record_data, c => +c.event_id)
				.then(function (id) {
					event_id = id
					return t.none(new_recording,{ id, unique_token });
				});
		}).then(function () {
			res.status(200)
				.json({
					status: 'success',
					upload_token: unique_token,
					url: `${event_id}/${unique_token}/`
				});
		})
		.catch(function (err) {
			return err;
		}).then(function (err) { 
			res.status(500)
				.json({
					status: 'error',
					msg: err,
					upload_token: null,
					url: null,
				});
		}).catch(function () {});
}
app.post('/upload/', upload);

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
	req.on('data', function (chunk) {
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
	req.on('end', function () {
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
	req.on('error', function (err) {
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
app.listen(3000, function () {
	console.log('Currently defending on port 3000!!')
});
