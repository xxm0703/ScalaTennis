CREATE TABLE IF NOT EXISTS user
(
    email         VARCHAR(255) PRIMARY KEY,
    password_hash TEXT                              NOT NULL,
    role          ENUM ("admin", "owner", "player") NOT NULL,
    name          TEXT                              NOT NULL,
    age           INT
);
