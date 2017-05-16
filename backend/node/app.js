var express = require('express')
const fs = require('fs')
var bodyParser = require("body-parser");
var pgp = require('pg-promise')({});
const uuid = require('uuid/v4');




var cn = {
    host: 'localhost',
    port: 5432,
    database: 'pdefender',
    user: 'node',
    password: 'password'
};
var db = pgp(cn);
var app = express()


/*******************
 *	Routes.
 */
// only parsing json automatically where needed.
app.post('/upload/', bodyParser.json())
app.post('/user/new/', bodyParser.json())
app.post('/nearby/', bodyParser.json())



/***********
 *	Helper function for setting up QueryFiles
 *	usage:
 *		var sqlFindUser = sql('./sql/findUser.sql');
 *	param:
 *		file: filename (string)
 */
function sql(file) {
    //path.join(__dirname, file)
    return new pgp.QueryFile(file, {minify: true});
}



/**********
 *	Setup QueryFiles once at startup
 */
var stopEvent = sql('./sql_queries/pd_event/update_status.sql');
var getNearby = sql('./sql_queries/pd_event/get_nearby.sql');
var createUser = sql('./sql_queries/pd_user/create.sql');






// boilerplate response -- returns all events 
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

// get nearby incidents
app.post('/nearby/', function (req, res, next) {
	var ip_regex = new RegExp('((?:[0-9]{1,3}\.?){4})');
	var r_method = req.method;
	var r_path = req.path;
	var r_ipaddr  = req.ip.match(ip_regex)[0];
	var date_now = Date.now();
	console.log(`[(${date_now})${r_method}]<${r_path}> ${r_ipaddr}`);
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
    }).then(function (err) {  // error resonses
		    res.status(500)
		        .json({
		        	status: 'error',
		          	msg: err,
		        })
	}).catch(function(err) {});
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

	user_promise = db.query(createUser, user)
		.then(function (result) {
			console.log(result);
			console.log(user_promise);
	      	res.status(200)
	        	.json({
	        	  	status: 'success',
	      		});
		}).catch(function (err) {
			console.log(err)
	    	//return err;
	    }).then(function (err) {  // error resonses
			    res.status(500)
			        .json({
			        	status: 'error',
			          	msg: err,
			        })
		}).catch(function() {});
});

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
	console.log(req.body)
	location = req.body.location 
	// check user credentials
	user = req.body.user /// @todo: change to actual credential lookup 
	var event_id
	unique_token = user + '_' + uuid()
	date = new Date()
	console.log(date.getTime())
	console.log(date.toUTCString())

	var record_data = {
        			 	user_id: user, 
        			 	geo: location, 
        			 	active: true, 
        			 	timestamp: date
        			  };

	db.tx(function (t) {
        return t.one('INSERT INTO  pd_event (pd_user_id, location, active, event_date) VALUES ($<user_id>, $<geo>, $<active>, $<timestamp>)  RETURNING event_id', 
        			 record_data,
					 c => +c.event_id )
            .then(function (id) {
            	event_id = id
            	console.log(id)
            	console.log(unique_token)
                return t.none('INSERT INTO pd_recording (event_id, filename) VALUES ($<id>, $<unique_token>)', {id, unique_token});
            });
    }).then(function () {
		res.status(200)
        .json({
          status: 'success',
          upload_token: unique_token,
          url: ""+event_id+"/"+unique_token+"/"
        });
    })
    .catch(function (err) {
      return err;
    }).then(function (err) {  // error resonses
	    res.status(500)
	        .json({
	          status: 'error',
	          msg: err,
	          upload_token: null,
	          url: null,
	        });
	   	}).catch(function() {});
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

app.post('/upload/:event/:id/', function(req, res) {
	file_id = req.params.id
	event = req.params.event
	console.log("Beginning transfer for unique key: " + file_id)	
	
	fileStream = fs.createWriteStream("./data_files/"+file_id + ".wav")
	req.pipe(fileStream)
	
	var tick = 0
	var total_bytes = 0;
	var errorMsg = null
	var stop_data = {
		active: false,
		event_id: event
	}

	// will be called any time data is in the buffer -- `chunk`
	req.on('data', function(chunk){ 
		//only logs data currently
		console.log(tick + ": " + (chunk.length / 1024).toFixed(2) + " KiB")
		total_bytes = total_bytes + chunk.length
		tick++; 
	})
	req.on('end', function(){
		db.none(stopEvent, stop_data)
			.then(() => { 
				console.log("Successful pd_event table update.");
			})
			.catch((err) => {
				console.log("Error updating pd_event table.");
				console.log(err);
			});

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