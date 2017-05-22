/*
    Create a user. 
*/

INSERT INTO  pd_user (email, google_id, google_firstname, google_lastname) 
VALUES ($<g_email>, $<g_userid>, $<f_name>, $<l_name>)  
RETURNING pd_user;