CREATE TABLE IF NOT EXISTS postponed_requests (
	id BIGSERIAL PRIMARY KEY,
	object_class VARCHAR NOT NULL,
	action VARCHAR NOT NULL,
	args VARCHAR,
	state VARCHAR,
	reason VARCHAR,
	last_send TIMESTAMP,
	retries INT
);