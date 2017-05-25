/** 
 * Attempt to select the user for the given google_id 
 */


SELECT * FROM pd_user 
WHERE google_id = $<google_id>;