var db = require('./db.js').connection;
var q = require('./sql.js');
var users = q.users;
var events = q.events;
var recordings = q.recordings;

const fs = require('fs');
var stream = require('stream');

/**
 * generate a unique id (for filenames)
 */
const uuid = require('uuid/v4');

/**
 * Just returns all events, with associated user and recordings
 */
function query_all (req, res, next) { 
	db.any(sql.get_all)
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

function get_users(req, res, next) {
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
				 console.error(err)
			})
		}); //end catch of db.any then 
};

/**
 * get a specific user's data
 */
function get_user(req, res, next){
	id = req.params.userid;
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
};

/**
 * Get all events and recordings associated with a user
 */
function get_user_events(req, res, next) {
	id = req.params.userid;
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
};


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
    console.log(req.body);
	var { current_location, user } = req.body
	var event
	date = new Date()
	var unique_token
	db.tx( (t) => {
        //console.log(t);
		return t.one(users.get_by_google_id, {google_id: user}, u => +u.user_id) //google_id --> pd_user_id
        .then( (u_id) => {
            console.log(u_id);
            unique_token = u_id + '_' + uuid()
            record_data = {
                user_id: u_id,
                geo: current_location,
                active: true,
                timestamp: date
            };
            console.log(record_data);
            return t.one(events.start_event, record_data, event => +event.event_id) //create event --> event_id
        }).then( (event_id) => {
            console.log("Event ID: " + event_id);
            event = event_id;
            recording_data = {
                event_id: event_id,
                filename: unique_token
            };
            return t.none(recordings.create, recording_data);
		}).then( () => {
			console.log(event, unique_token);
			res.status(200)
				.json({
					status: 'success',
					upload_token: unique_token, //token all by itself
					url: `${event}/${unique_token}/` //unique upload url
				});
		})
    })
    .catch( (err) => {
        console.log(err.message);
        res.status(500)
            .json({
                status: 'error',
                msg: err.message ? err.message : err, //if there is a more specific 'message' structure use that
                upload_token: null,
                url: null,
            });
    });
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
	console.log("Beginning transfer for unique key: " + file_id)

	fileStream = fs.createWriteStream("./data_files/" + file_id + ".pcm")
	req.pipe(fileStream);

    // setup stream event in case someone wants to listen live.
    live_streams[event] = new stream.Readable().on('stream_requested', (res) => {
        console.log("STREAM REQUESTED!!!!");
        //console.log(res);
        req.pipe(res);
    });


    var tick = 0
	var total_bytes = 0;
	var errorMsg = null
	var stop_data = {
		active: false,
		event_id: event
	};
	/**
	 * Called any time data is recieved from the request. Logs how much is transferred
	 * and also keeps track of the number of chunks. 
	 * @param {bytes[]} chunk
	 **/
	req.on('data', (chunk) => {
		console.log(tick + ": " + (chunk.length / 1024).toFixed(2) + " KiB")
		total_bytes = total_bytes + chunk.length
		tick++;
        // basically if someone is listening, write data
	})
	/**
	 * Called when the connection / request is ended. 
	 **/
	req.on('end',  ( ) => {
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
		console.log("") //newline for prettier output on console
		//live_streams['1'].end;
	});
	req.on('error', (err) => {
        live_streams[event] = null;
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

/**
 * Stream live events
 */
var live_streams = {};

function listen_stream (req, res) {
	id = req.params.eventid;
    if (live_streams[id] != null ) {
        console.log(live_streams[id]);
        live_streams[id].emit('stream_requested', res);
    }
    else {
        res.sendStatus(404);
    }
};


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