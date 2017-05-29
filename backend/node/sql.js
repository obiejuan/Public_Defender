/** @author Bryan McCoid  bmccoid_at_ucsc.edu */

var promise = require('bluebird');
var pgp = require('pg-promise')({ promiseLib: promise });

/**
 * Helper function to return QueryFiles from filenames.
 * @example
 * //returns QueryFile of findUser.sql file
 * var sqlFindUser = sql('./sql/findUser.sql');
 * 
 * @param {string} file The filename of the *.sql file containing the query.
 * @returns {Queryfile|object} the Queryfile object for use in db.query, etc.., function
 */
function sql(file) {
	return new pgp.QueryFile(file, { minify: true });
}



var get_all = sql('./sql_queries/get_all.sql');
var stopEvent = sql('./sql_queries/pd_event/update_status.sql');
var getNearby = sql('./sql _queries/pd_event/get_nearby.sql');
var init_upload = sql('./sql_queries/pd_event/init_upload.sql');
var new_recording = sql('./sql_queries/pd_recording/new_recording.sql');
var select_user = sql('./sql_queries/pd_user/check_usr_exists.sql');
var create_user = sql('./sql_queries/pd_user/create_usr.sql');
var get_user_events = sql('./sql_queries/pd_user/get_user_events.sql');
var get_user = sql('./sql_queries/pd_user/get.sql');


module.exports = {
    queryfile_from_string: sql,
    get_all: get_all,
    users: { 
        get: get_user,
        get_by_google_id: select_user,
        create: create_user,
        get_events: get_user_events
     },
     events: {
        stop_event: stopEvent,
        get_nearby: getNearby,
        start_event: init_upload
     },
     recordings: { create: new_recording }
}