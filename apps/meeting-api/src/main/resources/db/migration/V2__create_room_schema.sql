CREATE TABLE room (
 id uuid PRIMARY KEY,
 slot smallint NOT NULL CHECK (slot BETWEEN 1 AND 10),
 name varchar(100) NOT NULL CHECK(length(trim(name))>0),
 description varchar(2000),
 location varchar(100) NOT NULL,
 floor varchar(30) NOT NULL,
 capacity integer NOT NULL CHECK(capacity BETWEEN 1 AND 200),
 equipment jsonb NOT NULL DEFAULT '[]'::jsonb CHECK(jsonb_typeof(equipment)='array'),
 is_active boolean NOT NULL DEFAULT true,
 version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 created_by uuid NOT NULL REFERENCES user_account(id),
 updated_by uuid NOT NULL REFERENCES user_account(id)
);
CREATE UNIQUE INDEX idx_room_slot_active_unique ON room(slot) WHERE is_active;
CREATE UNIQUE INDEX idx_room_name_active_unique ON room(lower(name)) WHERE is_active;
