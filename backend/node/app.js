var express = require('express')
const fs = require('fs')
var bodyParser = require("body-parser");
var app = express()

// only parsing json automatically where needed.
app.post('/upload/', bodyParser.json())


// boilerplate response
app.get('/', function (req, res) {
  res.send('Hello World!')
  console.log(req)
})

app.post('/upload/', function(req, res) {
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
	console.log(req.body)
	
	// placeholder response
	res.send({	
				"upload_token": "uniquekey",
				"url": "/upload/uniquekey"
			})
})

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
	
	fileStream = fs.createWriteStream('test.png')
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