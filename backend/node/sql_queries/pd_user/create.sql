/*
	Create a new user
*/

INSERT INTO pd_user (auth_key, email) 
VALUES ($<auth_key>, $<email>);