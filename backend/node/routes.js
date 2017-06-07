var views = require('./views.js');
var bodyParser = require('body-parser');

/**
 *	Routes: Only parsing json automatically where needed.
 */
module.exports = (app) => {
// no-category
app.post('/user/', bodyParser.json())
app.get('/', views.query_all);

// nearby api
app.post('/nearby/', bodyParser.json(), views.nearby);

// user level api
app.get('/user/:userid/events/', views.users.get_events);
app.get('/user/:userid/', views.users.get);
app.get('/users/', views.users.get_all);

// event/recording level api
app.post('/upload/', bodyParser.json(), views.events.begin_upload);
app.post('/upload/:event/:id/', views.events.get_stream);

// test streaming api
app.get('/stream/:eventid/', views.stream);

}