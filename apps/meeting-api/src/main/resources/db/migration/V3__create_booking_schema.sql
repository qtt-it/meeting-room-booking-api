CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE booking (
 id uuid PRIMARY KEY,
 room_id uuid NOT NULL REFERENCES room(id),
 organizer_id uuid NOT NULL REFERENCES user_account(id),
 title varchar(150) NOT NULL CHECK(length(trim(title))>0),
 description varchar(2000),
 status varchar(12) NOT NULL CHECK(status IN ('pending','approved','rejected','cancelled')),
 recurrence jsonb NOT NULL DEFAULT 'null'::jsonb,
 decided_by uuid REFERENCES user_account(id),
 decision_reason varchar(1000),
 version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 created_by uuid NOT NULL REFERENCES user_account(id),
 updated_by uuid NOT NULL REFERENCES user_account(id),
 CONSTRAINT uq_booking_id_room UNIQUE (id,room_id)
);
CREATE INDEX idx_booking_organizer_created_at ON booking(organizer_id,created_at DESC);
CREATE INDEX idx_booking_status_created_at ON booking(status,created_at DESC);

CREATE TABLE booking_occurrence (
 id uuid PRIMARY KEY,
 booking_id uuid NOT NULL,
 room_id uuid NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 status varchar(12) NOT NULL CHECK(status IN ('pending','approved','rejected','cancelled')),
 created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT fk_booking_occurrence_booking FOREIGN KEY(booking_id,room_id) REFERENCES booking(id,room_id),
 CONSTRAINT ck_booking_occurrence_window CHECK(starts_at<ends_at),
 CONSTRAINT ex_booking_occurrence_no_overlap EXCLUDE USING gist (
   room_id WITH =,
   tstzrange(starts_at,ends_at,'[)') WITH &&
 ) WHERE (status='approved')
);
CREATE INDEX idx_booking_occurrence_booking_id ON booking_occurrence(booking_id);
CREATE INDEX idx_booking_occurrence_room_start ON booking_occurrence(room_id,starts_at);

CREATE TABLE booking_attendee (
 booking_id uuid NOT NULL REFERENCES booking(id),
 user_account_id uuid NOT NULL REFERENCES user_account(id),
 response varchar(12) NOT NULL DEFAULT 'pending' CHECK(response IN ('pending','accepted','declined')),
 responded_at timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(booking_id,user_account_id)
);
CREATE INDEX idx_booking_attendee_user_account_id ON booking_attendee(user_account_id);

CREATE TABLE booking_audit (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
 booking_id uuid NOT NULL REFERENCES booking(id),
 actor_id uuid NOT NULL REFERENCES user_account(id),
 action varchar(50) NOT NULL,
 details varchar(1000),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_booking_audit_booking_created_at ON booking_audit(booking_id,created_at);
