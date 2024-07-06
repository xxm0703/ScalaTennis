ALTER TABLE notification ADD COLUMN targetUser VARCHAR(255);
ALTER TABLE notification ADD CONSTRAINT fk_targetUser FOREIGN KEY (targetUser) REFERENCES user (email);



