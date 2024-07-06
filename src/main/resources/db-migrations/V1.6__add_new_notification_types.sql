ALTER TABLE notification 
MODIFY COLUMN notification_type ENUM(
    'club_creation_request',
    'court_creation_request',
    'reservation_creation_request',
    'reservation_cancelled',
    'club_creation_request_denied',
    'court_creation_request_denied',
    'reservation_deleted',
    'reservation_approved',
    'club_created',
    'court_created',
    'court_updated',
    'club_transferred'
) NOT NULL;
