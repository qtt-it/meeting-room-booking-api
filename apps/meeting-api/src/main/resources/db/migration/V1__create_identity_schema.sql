CREATE TABLE system_lock (id varchar(30) PRIMARY KEY);
INSERT INTO system_lock(id) VALUES ('identity'), ('room');

CREATE TABLE user_account (
 id uuid PRIMARY KEY,
 email varchar(254) NOT NULL UNIQUE,
 display_name varchar(100) NOT NULL CHECK (length(trim(display_name)) > 0),
 password_hash varchar(100) NOT NULL,
 role varchar(10) NOT NULL CHECK (role IN ('user','admin')),
 is_active boolean NOT NULL DEFAULT true,
 version bigint NOT NULL DEFAULT 0,
 security_version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 created_by uuid NOT NULL,
 updated_by uuid NOT NULL,
 CONSTRAINT ck_user_account_email_normalized CHECK (email=lower(trim(email)))
);

CREATE TABLE refresh_family (
 id uuid PRIMARY KEY,
 user_account_id uuid NOT NULL REFERENCES user_account(id),
 revoked_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_family_user_account_id ON refresh_family(user_account_id);

CREATE TABLE refresh_session (
 id uuid PRIMARY KEY,
 user_account_id uuid NOT NULL REFERENCES user_account(id),
 family_id uuid NOT NULL REFERENCES refresh_family(id),
 token_hash varchar(64) NOT NULL UNIQUE,
 security_version bigint NOT NULL,
 expires_at timestamptz NOT NULL,
 revoked_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_session_family_id ON refresh_session(family_id);
CREATE INDEX idx_refresh_session_user_account_id ON refresh_session(user_account_id);
CREATE INDEX idx_refresh_session_expires_at ON refresh_session(expires_at);

CREATE TABLE activity_audit (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 actor_id uuid NOT NULL,
 action varchar(50) NOT NULL,
 resource_type varchar(30) NOT NULL,
 resource_id uuid NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_activity_audit_resource ON activity_audit(resource_type,resource_id,created_at);
