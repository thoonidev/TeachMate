-- Existing installations only. Back up first and stop the application.
-- Resolve duplicate email addresses before adding these constraints.
-- Apply once; new installations already have these definitions in schema.sql.
ALTER TABLE user_details MODIFY clave VARCHAR(255) NOT NULL;
ALTER TABLE docente MODIFY clave VARCHAR(255) NOT NULL;
ALTER TABLE user_details ADD CONSTRAINT uq_user_email UNIQUE (email);
ALTER TABLE docente ADD CONSTRAINT uq_docente_email UNIQUE (email);
-- Next run PasswordTool migrate with DB_* pointing to this database.
