var express = require('express');
var middlewares = require('./middleware.js');
/**
 * Setup Express.js
 */
var app = express()

/**
 * Setup middleware
 */
app.use(middlewares.logging, middlewares.login) //middlewares.login

/**
 * Load urls
 */
var urls = require('./routes.js')(app);

/** 
 * Run the actual server
 **/
app.listen(3000, () => {
	console.log('Currently defending on port 3000!!')
});


//doubt we really need this but just in case
module.exports = {
	app: app
}