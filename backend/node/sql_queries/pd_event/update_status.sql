/******
 *	Update pd_event status
 */
 
UPDATE pd_event 
SET active=$<active>
WHERE event_id=$<event_id>;