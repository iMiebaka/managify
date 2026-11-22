-- Optional profile image for a worker (a link to an image hosted elsewhere).
ALTER TABLE workers
    ADD COLUMN image_url VARCHAR(1000) NULL AFTER keycloak_user_id;
