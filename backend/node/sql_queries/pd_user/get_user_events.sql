SELECT * FROM pd_event 
INNER JOIN pd_recording ON pd_event.event_id=pd_recording.event_id 
WHERE pd_event.pd_user_id=$<user_id>