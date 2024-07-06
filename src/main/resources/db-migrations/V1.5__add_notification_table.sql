CREATE TABLE IF NOT EXISTS notification (
    id VARCHAR(255) PRIMARY KEY,
    notification_type ENUM(
        'club_creation_request',
        'court_creation_request',
        'reservation_creation_request',
        'reservation_cancelled',
        'club_creation_request_denied',
        'court_creation_request_denied',
        'reservation_deleted'
    ) NOT NULL,
    triggered_by  VARCHAR(255) NOT NULL,
    club_id  VARCHAR(255),
    court_id  VARCHAR(255),
    reservation_id  VARCHAR(255),
    status ENUM('read', 'not_read') NOT NULL,
    created_at TIMESTAMP NOT NULL,
    target_user  VARCHAR(255) 
);
