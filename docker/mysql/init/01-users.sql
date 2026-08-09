-- Users applicatifs a droits restreints (exigence SGBD).
-- Aucun user "tout puissant" n'est utilise par l'application.

CREATE USER IF NOT EXISTS 'padel_ddl'@'%'      IDENTIFIED BY 'ddl_pwd';
CREATE USER IF NOT EXISTS 'padel_app'@'%'      IDENTIFIED BY 'app_pwd';
CREATE USER IF NOT EXISTS 'padel_readonly'@'%' IDENTIFIED BY 'read_pwd';

-- padel_ddl : uniquement le DDL, utilise par le script d'init ci-dessous.
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES ON padel.* TO 'padel_ddl'@'%';

-- padel_app : le user de l'application. DML seulement, aucun droit de schema.
GRANT SELECT, INSERT, UPDATE, DELETE ON padel.* TO 'padel_app'@'%';

-- padel_readonly : lecture seule, pour les exports et rapports.
GRANT SELECT ON padel.* TO 'padel_readonly'@'%';

FLUSH PRIVILEGES;
