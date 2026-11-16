-- Corré esto UNA vez contra tu MySQL local, como root:
--   mysql -u root -p < scripts/bootstrap-mysql.sql
-- Crea un usuario de desarrollo dedicado (no uses tu root real en la app).
CREATE DATABASE IF NOT EXISTS skillswap CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'skillswap'@'localhost' IDENTIFIED BY 'skillswap_dev_2024';
GRANT ALL PRIVILEGES ON skillswap.* TO 'skillswap'@'localhost';
FLUSH PRIVILEGES;
