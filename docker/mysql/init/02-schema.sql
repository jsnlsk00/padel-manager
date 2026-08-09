-- Schema relationnel. Execute automatiquement par le conteneur MySQL au premier demarrage.
-- L'application (padel_app) n'a AUCUN droit DDL : elle ne peut pas alterer ce schema.

USE padel;

CREATE TABLE site (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(120) NOT NULL,
  address       VARCHAR(200) NOT NULL,
  opening_time  TIME         NOT NULL,
  closing_time  TIME         NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_site_name UNIQUE (name),
  CONSTRAINT ck_site_hours CHECK (closing_time > opening_time)
) ENGINE = InnoDB;

CREATE TABLE court (
  id        BIGINT NOT NULL AUTO_INCREMENT,
  number    INT    NOT NULL,
  site_id   BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_court_site_number UNIQUE (site_id, number),
  CONSTRAINT fk_court_site FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE site_closure (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  closed_on   DATE         NOT NULL,
  reason      VARCHAR(200) NOT NULL,
  site_id     BIGINT       NULL, -- NULL = fermeture globale du reseau
  PRIMARY KEY (id),
  CONSTRAINT fk_closure_site FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE member (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  matricule      VARCHAR(10)  NOT NULL,
  first_name     VARCHAR(80)  NOT NULL,
  last_name      VARCHAR(80)  NOT NULL,
  email          VARCHAR(160) NOT NULL,
  password_hash  VARCHAR(100) NOT NULL,
  type           VARCHAR(10)  NOT NULL,
  home_site_id   BIGINT       NULL,
  admin_site_id  BIGINT       NULL,
  balance_due    DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  banned_until   DATE         NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_member_matricule UNIQUE (matricule),
  CONSTRAINT uk_member_email UNIQUE (email),
  CONSTRAINT fk_member_home_site  FOREIGN KEY (home_site_id)  REFERENCES site (id),
  CONSTRAINT fk_member_admin_site FOREIGN KEY (admin_site_id) REFERENCES site (id),
  CONSTRAINT ck_member_type CHECK (type IN ('GLOBAL', 'SITE', 'FREE')),
  CONSTRAINT ck_member_balance CHECK (balance_due >= 0)
) ENGINE = InnoDB;

CREATE TABLE member_role (
  member_id BIGINT      NOT NULL,
  role      VARCHAR(24) NOT NULL,
  PRIMARY KEY (member_id, role),
  CONSTRAINT fk_member_role_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE,
  CONSTRAINT ck_member_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN_SITE', 'ROLE_ADMIN_GLOBAL'))
) ENGINE = InnoDB;

CREATE TABLE match_booking (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  court_id      BIGINT       NOT NULL,
  organizer_id  BIGINT       NOT NULL,
  start_time    DATETIME     NOT NULL,
  end_time      DATETIME     NOT NULL,
  visibility    VARCHAR(10)  NOT NULL,
  status        VARCHAR(12)  NOT NULL,
  price         DECIMAL(8,2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_match_court_start UNIQUE (court_id, start_time),
  CONSTRAINT fk_match_court     FOREIGN KEY (court_id)     REFERENCES court (id),
  CONSTRAINT fk_match_organizer FOREIGN KEY (organizer_id) REFERENCES member (id),
  CONSTRAINT ck_match_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
  CONSTRAINT ck_match_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'PLAYED')),
  CONSTRAINT ck_match_period CHECK (end_time > start_time)
) ENGINE = InnoDB;

CREATE INDEX ix_match_start ON match_booking (start_time);

CREATE TABLE match_participation (
  id         BIGINT  NOT NULL AUTO_INCREMENT,
  match_id   BIGINT  NOT NULL,
  player_id  BIGINT  NOT NULL,
  paid       BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  CONSTRAINT uk_participation UNIQUE (match_id, player_id),
  CONSTRAINT fk_participation_match  FOREIGN KEY (match_id)  REFERENCES match_booking (id) ON DELETE CASCADE,
  CONSTRAINT fk_participation_player FOREIGN KEY (player_id) REFERENCES member (id)
) ENGINE = InnoDB;

CREATE TABLE payment (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  match_id           BIGINT       NULL,
  payer_id           BIGINT       NOT NULL,
  amount             DECIMAL(8,2) NOT NULL,
  paid_at            DATETIME     NOT NULL,
  is_balance_payment BOOLEAN      NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id),
  CONSTRAINT fk_payment_match FOREIGN KEY (match_id) REFERENCES match_booking (id) ON DELETE SET NULL,
  CONSTRAINT fk_payment_payer FOREIGN KEY (payer_id) REFERENCES member (id),
  CONSTRAINT ck_payment_amount CHECK (amount > 0)
) ENGINE = InnoDB;
