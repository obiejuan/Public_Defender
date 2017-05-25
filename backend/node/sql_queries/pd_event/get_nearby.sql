/******
 *	Get nearby events that are of distance 'distance'
 *	away from the input 'current_location' ordered by the 
 *  radius distance.
 *
 *	@author: Bryan M.
 */
 
SELECT event_id, location, active, point($<current_location>) <@> pd_event.location AS event_dist
FROM pd_event 
WHERE (point($<current_location>) <@> pd_event.location) < ($<distance>)
AND pd_event.active=TRUE
ORDER by event_dist;