ALTER TABLE reservation ADD COLUMN reservation_status ENUM ("placed", "approved", "cancelled") NOT NULL;