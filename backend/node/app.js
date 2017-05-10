var express = require('express')
const fs = require('fs')
var bodyParser = require("body-parser");
var pgp = require('pg-promise')({});
const uuid = require('uuid/v4');
//var connectionString = 'postgres://localhost:5432/pdefender-dev';



var cn = {
    host: 'localhost',
    port: 5432,
    database: 'pdefender',
    user: 'node',
    password: 'password'
};
var db = pgp(cn);

var app = express()

// only parsing json automatically where needed.
app.post('/upload/', bodyParser.json())
app.post('/user/new/', bodyParser.json())
app.post('/nearby/', bodyParser.json())




/*	Table def (not exact sql):

	CREATE TABLE public.pd_event
	(
	  id integer Primary Key,
	  filename character varying(255),
	  location point,   <---- Points are taken as (longitude, latitude)
	)

	CREATE TABLE public.pd_user
	(
	  id integer Primary Key,
	  auth_key character varying(255),
	  email character varying(255),
	)

	assuming we are at: (-97.515678, 35.512363)
	others at:

	-97.510185, 35.496025
	-97.508039, 35.523762
	-98.473206, 35.866413
	-97.505035, 35.484388

	SELECT *, point(-97.515678, 35.512363) <@> pd_event.location AS event_dist
	FROM pd_event
	WHERE (point(-97.515678, 35.512363) <@> pd_event.location) < 10 
	ORDER by event_dist;
	*** 10 is the distance in miles ^^^^ ***

*/

// boilerplate response
app.get('/', function (req, res, next) {
  db.any('select * FROM pd_user INNER JOIN pd_event ON pd_event.pd_user_id=pd_user.id')
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

// get nearby incidents
app.post('/nearby/', function (req, res, next) {
	current_location = req.body.location
	distance = req.body.distance
  	db.any('SELECT id, location, active, point($1) <@> pd_event.location AS event_dist FROM pd_event WHERE (point($1) <@> pd_event.location) < ($2) ORDER by event_dist;', 
  	[ current_location, distance ])
    .then(function (data) {
      	res.status(200)
        	.json({
          	  status: 'success',
          	  data: data,
        	});
    })
    .catch(function (err) {
      	return next(err);
    });
})

/*****
 *	                                  Table "public.pd_user"
  Column  |          Type          |                      Modifiers                       
----------+------------------------+------------------------------------------------------
 user_id  | integer                | not null default nextval('pd_user_id_seq'::regclass)
 auth_key | character varying(255) | 
 email    | character varying(255) | 
Indexes:
    "pd_user_pkey" PRIMARY KEY, btree (user_id)
Referenced by:
    TABLE "pd_event" CONSTRAINT "pd_user_id" FOREIGN KEY (pd_user_id) REFERENCES pd_user(user_id)
 *
 */
app.post('/user/new/', function(req, res, next){
	user = req.body
	user_promise = db.query('INSERT INTO pd_user (auth_key, email) VALUES ($1, $2)',  [ user.auth_key, user.email ])
	.then(function (response_db) {
	  db.any('select * from pd_user').then(function (data) {
      res.status(200)
        .json({
          status: 'success',
          data: data,
        });
        })
    })
    .catch(function (err) {
      return next(err);
    });
})

/* 
 *   Table "public.pd_event"
	   Column   |            Type             |                       Modifiers                       
	------------+-----------------------------+-------------------------------------------------------
	 event_id   | integer                     | not null default nextval('pd_event_id_seq'::regclass)
	 location   | point                       | 
	 pd_user_id | integer                     | not null
	 active     | boolean                     | not null default true
	 event_date | timestamp without time zone | not null
	Indexes:
	    "pd_event_pkey" PRIMARY KEY, btree (event_id)
	    "fki_pd_user_id" btree (pd_user_id)
	Foreign-key constraints:
	    "pd_user_id" FOREIGN KEY (pd_user_id) REFERENCES pd_user(user_id)
	Referenced by:
	    TABLE "pd_recording" CONSTRAINT "pd_recordings_event_id_fkey" FOREIGN KEY (event_id) REFERENCES pd_event(event_id)
 *
 */
app.post('/upload/', function(req, res, next) {
	/*******************************************************
		Used as authentication and handshake to begin file
		upload below. Steps required:
			1.) User authentication via JSON file
			2.) Verify information
			3.) Create database entrie(s) related to the event
			4.) Respond with secret (and unique) key for actual file upload

		NOTES: Geolocation data should probably be handled here as well
		to minimize network requests. 
	*******************************************************/
	// filename goes to pd_recording 


	console.log(req.body)
	location = req.body.location 
	// check user credentials
	user = req.body.user /// @todo: change to actual credential lookup 

	unique_token = user + '_' + uuid()
	date = new Date()
	console.log(date)
	console.log(date.toUTCString())
	db.tx(function (t) {
        return t.one('INSERT INTO  pd_event (pd_user_id, location, active, event_date) VALUES ($1, $2, $3, $4)  RETURNING event_id', [user, location, true, date], c => +c.event_id)
            .then(function (id) {
                return t.none('INSERT INTO pd_recording (event_id, filename, )', [id, unique_token]);
            });
    }).then(function () {
		res.status(200)
        .json({
          status: 'success',
          upload_token: unique_token,
          url: "/upload/"+unique_token,
        });
    })
    .catch(function (err) {
      return next(err);
    });
})

/*
	Table "public.pd_recording"
  Column  |          Type          | Modifiers 
----------+------------------------+-----------
 event_id | integer                | not null
 filename | character varying(255) | not null
Indexes:
    "pd_recordings_pkey" PRIMARY KEY, btree (event_id, filename)
Foreign-key constraints:
    "pd_recordings_event_id_fkey" FOREIGN KEY (event_id) REFERENCES pd_event(event_id)
*/

app.post('/upload/:id', function(req, res) {
	/*******************************************************
	 	Post file using `id` from /upload/ by streaming
	 	it in a POST body. Steps required:
	 		1.) Lookup `id` in db and verify information
	 		2.) Create filestream 
	 		3.) Hook request body stream into filestream 
	 		4.) Respond with status 
	*******************************************************/

	id = req.params.id
	console.log("Beginning transfer for unique key: " + id)	
	
	fileStream = fs.createWriteStream(id + ".wav")
	req.pipe(fileStream)
	
	var tick = 0
	var total_bytes = 0;
	var errorMsg = null

	// will be called any time data is in the buffer -- `chunk`
	req.on('data', function(chunk){ 
		//only logs data currently
		console.log(tick + ": " + (chunk.length / 1024).toFixed(2) + " KiB")
		total_bytes = total_bytes + chunk.length
		tick++; 
	})
	req.on('end', function(){
		console.log("End: "+ (total_bytes / 1024).toFixed(2) + " KiB transfered.")
		console.log("") //newline for prettier output on console
	})
	req.on('error', function(err){
		errorMsg = err
		console.log("Error: " + err)
	})
	res.send({	
				"UploadCompleted": true,
				"Error": errorMsg,
			})

	
})

app.listen(3000, function () {
  console.log('Currently defending on port 3000!')
})