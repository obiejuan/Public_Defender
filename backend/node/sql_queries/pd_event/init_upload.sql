/*
    Create an event.
*/

INSERT INTO  pd_event (pd_user_id, location, active, event_date) 
VALUES ($<user_id>, $<geo>, $<active>, $<timestamp>)  
RETURNING event_id