/** @author Bryan McCoid  bmccoid_at_ucsc.edu */

var express = require("express");
var middlewares = require("./middleware.js");
var console = require("console");

/**
 * Setup Express.js
 */
var app = express();

/**
 * Setup middleware
 * @param  {} middlewares.logging
 * @param  {} middlewares.login
 */
app.use(middlewares.logging, middlewares.login); //middlewares.login

/**
 * Load urls
 * 
 * @param  {} "./routes.js"
 * @param  {} app
 */
require("./routes.js")(app);

/** 
 * Run the actual server
 **/
app.listen(3000, () => {
	console.log("Currently defending on port 3000!!");
});

//doubt we really need this but just in case

module.exports = { app: app };