/** @author Bryan McCoid  bmccoid_at_ucsc.edu */

// requirements and imports
var db = require('./db.js').connection;
var q = require('./sql.js');
var users = q.users;
var events = q.events;
var recordings = q.recordings;
const fs = require('fs');
var stream = require('stream');
var console = require("console");


const uuid = require('uuid/v4');

//Just returns all events, with associated user and recordings.
function query_all (req, res, next) { 
	db.any(q.get_all)
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
}

/**
 * Get's the nearby active=true events for the parameters in request.
 * 
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 * 
 * JSON keys: 
 * 	@param {string} current_location
 * 	@param {int} distance
 */
function nearby(req, res) {
	var query = {
		current_location: req.body.current_location,
		distance: req.body.distance
	};

	db.any(events.get_nearby, query)
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
		}).catch( (err) => {
			console.error(err);
		});
}

/**
 * Lists all Users:
 *	Table "public.pd_user"
 *		Name:	    | 	Datatype:		              |  other..
 *	----------------+---------------------------------+-----------------
 *	user_id         | 	integer                       |  primary key
 *	auth_key	    | 	character varying(255)        |
 *	email		    | 	character varying(255)        |
 *	google_id	    | 	character varying(255)        |
 *	google_firstname| 	character varying(255)        |
 *	google_lastname |   character varying(255)        |
 *	date_created	|   timestamp with time zone      |  auto-filled
 * 
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 */
function get_users(req, res) {
	db.any('SELECT * FROM pd_user').then( (data) => {
		console.log(data);
			res.status(200)
				.json({
					status: 'success',
					data: data
				});
		}).catch( (err) => {
			console.error(err)
			res.status(500)
				.json({
					status: 'error',
					msg: err,
			}).catch( (err) => {
				console.error(err);
			})
		}); 
}

/**
 * 
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 */
function get_user(req, res){
	var id = req.params.userid;
	db.any(users.get, {user_id: id}).then( (data) => {
			console.log(data);
			res.status(200)
				.json({
					status: 'success',
					data: data
				});
	}).catch( (err) => {
		console.error(err)
		res.status(500)
			.json({
				status: 'error',
				msg: err,
		}).catch( (err) => {
			console.error(err)
		})
	}); 
}

/**
 * @deprecated
 * 
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 */
function get_user_events(req, res) {
	var id = req.params.userid;
	db.any(users.get_events, {user_id: id}).then( (data) => {
			console.log(data);
			res.status(200)
				.json({
					status: 'success',
					data: data
				});
	}).catch( (err) => {
		console.error(err)
		res.status(500)
			.json({
				status: 'error',
				msg: err,
		}).catch( (err) => {
			console.error(err)
		})
	});
}

/**
 * 
 * Table "public.pd_recording"
 *		Column    |         Type           | Modifiers  
 *		----------+------------------------+-----------
 *		event_id  | integer                | not null
 *		filename  | character varying(255) | not null
 *
 * @file init_upload.sql
 * 
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 */
function upload (req, res) {
    console.log(req.body);
	var { current_location, user } = req.body
	var event
	var date = new Date()
	var unique_token
	db.tx( (t) => {
		// google_id --> pd_user_id
		return t.one(users.get_by_google_id, {google_id: user}, u => +u.user_id) 
        .then( (u_id) => {
            console.log(u_id);
            unique_token = u_id + '_' + uuid()
            var record_data = {
                user_id: u_id,
                geo: current_location,
                active: true,
                timestamp: date
            };
            console.log(record_data);
			// create event --> event_id
            return t.one(events.start_event, record_data, event => +event.event_id) 
        }).then( (event_id) => {
            console.log("Event ID: " + event_id);
            event = event_id;
            var recording_data = {
                event_id: event_id,
                filename: unique_token
            };
            return t.none(recordings.create, recording_data);
		}).then( () => {
			console.log(event, unique_token);
			res.status(200)
				.json({
					status: 'success',
					//token all by itself
					upload_token: unique_token, 
					//unique upload url
					url: `${event}/${unique_token}/` 
				});
		})
    })
    .catch( (err) => {
        console.log(err.message);
        res.status(500)
            .json({
                status: 'error',
				//if there is a more specific 'message' structure use that
                msg: err.message ? err.message : err, 
                upload_token: null,
                url: null,
            });
    });
}

/**
 * Main stream recieving function. After the stream is over with it changes the
 * active=true to active=false in the pd_event table.
 * 
 * Table "public.pd_event"
 *
 *	   Column   |            Type             |      Modifiers                         
 *	------------+-----------------------------+-------------------------
 *	 event_id   | integer                     | primary key counts up
 *	 location   | point                       | 
 *	 pd_user_id | integer                     | 
 *	 active     | boolean                     | 
 *	 event_date | timestamp without time zone |  
 *
 *
 * @param {object} req - The Express_js request object.
 * @param {object} res - The Express_js response object.
 * @return {void} - Doesn't return anything.
 */
function recieve_stream (req, res) {
	var file_id = req.params.id
	var event = req.params.event
	console.log("Beginning transfer for unique key: " + file_id)
	var fileStream = fs.createWriteStream("./data_files/" + file_id + ".pcm")
	req.pipe(fileStream);
    
    live_streams[event] = new stream.Readable().on('stream_requested', (res) => { // setup stream event in case someone wants to listen live.
        console.log("Stream requested.");
        req.pipe(res);
    });

    var tick = 0
	var total_bytes = 0;
	var errorMsg = null
	var stop_data = {
		active: false,
		event_id: event
	};
	req.on('data', (chunk) => {
		console.log(tick + ": " + (chunk.length / 1024).toFixed(2) + " KiB")
		total_bytes = total_bytes + chunk.length
		tick++;   
	})
	req.on('end',  ( ) => { // Called when the connection / request is ended. 
        console.log(event)
        console.log(stop_data)
		db.none(events.stop_event, stop_data)
			.then( ( ) => {
				console.log("Successful pd_event table update.");
			})
			.catch((err) => {
				console.log("Error updating pd_event table.");
				console.log(err);
			});
        live_streams[event] = null;
		console.log("End: " + (total_bytes / 1024).toFixed(2) + " KiB transfered.")
		console.log("") // Newline for prettier output on console
	});
	req.on('error', (err) => {
        live_streams[event] = null;
		errorMsg = err
		console.log("Error: " + err)
	});
	res.send({ // Send response to client. 
		"UploadCompleted": true,
		"Error": errorMsg,
	});
}

var live_streams = {};
function listen_stream (req, res) { // Stream live events
	var id = req.params.eventid;
    if (live_streams[id] != null ) {
        console.log(live_streams[id]);
        live_streams[id].emit('stream_requested', res);
    }
    else {
        res.sendStatus(404);
    }
}

// Export views for use elsewhere.
module.exports = {
    query_all: query_all,
    nearby: nearby,
    stream: listen_stream,
    users: {
        get_all: get_users,
        get_events: get_user_events,
        get: get_user
    },
    events: {
        begin_upload: upload,
        get_stream: recieve_stream
    }
}