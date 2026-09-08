ALTER TABLE users
    DROP COLUMN username;

ALTER TABLE users
    ADD COLUMN email VARCHAR(254),
    ADD COLUMN password_hash VARCHAR(255);

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL,
    ADD CONSTRAINT uk_users_email UNIQUE (email);
