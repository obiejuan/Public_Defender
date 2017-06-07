
SELECT * FROM pd_user 
INNER JOIN pd_event ON pd_event.pd_user_id=pd_user.user_id 
INNER JOIN pd_recording ON pd_event.event_id=pd_recording.event_id 

/*
WHERE pd_event.active=false
*/