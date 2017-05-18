var express = require('express')
const fs = require('fs')
var bodyParser = require("body-parser");
var pgp = require('pg-promise')({});
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
app.post('/user/new/', bodyParser.json())
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
	}
	console.log(query)
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
app.post('/user/new/', function (req, res, next) {
	user = req.body
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
	location = req.body.location
	user = req.body.user 
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
					console.log(id)
					console.log(unique_token)
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
	})
	req.on('error', function (err) {
		errorMsg = err
		console.log("Error: " + err)
	})
	/**
	 * Send response to client. Minimal currently.
	 **/
	res.send({
		"UploadCompleted": true,
		"Error": errorMsg,
	})
}
app.post('/upload/:event/:id/', recieve_stream)


/** 
 * Run the actual server
 **/
app.listen(3000, function () {
	console.log('Currently defending on port 3000!')
})