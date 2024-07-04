CREATE TABLE IF NOT EXISTS club
(
    id          VARCHAR(255) PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS court
(
    id      VARCHAR(255) PRIMARY KEY,
    name    TEXT                           NOT NULL,
    surface ENUM ("clay", "hard", "grass") NOT NULL,
    club_id VARCHAR(255)                   NOT NULL,
    FOREIGN KEY (club_id) REFERENCES club (id)
);



