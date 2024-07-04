CREATE TABLE IF NOT EXISTS reservation
(
    id                VARCHAR(255) PRIMARY KEY,
    user_id           VARCHAR(255) NOT NULL,
    court_id          VARCHAR(255) NOT NULL,
    start_time        TIMESTAMP    NOT NULL,
    placing_timestamp TIMESTAMP    NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user (email),
    FOREIGN KEY (court_id) REFERENCES court (id)
);

