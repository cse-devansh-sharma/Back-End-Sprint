CREATE DATABASE IF NOT EXISTS tms_auth_db;
CREATE DATABASE IF NOT EXISTS tms_timesheet_db;
CREATE DATABASE IF NOT EXISTS tms_leave_db;
CREATE DATABASE IF NOT EXISTS tms_admin_db;
CREATE DATABASE IF NOT EXISTS tms_notification_db;

CREATE USER IF NOT EXISTS 'tms_auth_user'@'%'
    IDENTIFIED BY 'auth_pass_123';
CREATE USER IF NOT EXISTS 'tms_ts_user'@'%'
    IDENTIFIED BY 'ts_pass_123';
CREATE USER IF NOT EXISTS 'tms_leave_user'@'%'
    IDENTIFIED BY 'leave_pass_123';
CREATE USER IF NOT EXISTS 'tms_admin_user'@'%'
    IDENTIFIED BY 'admin_pass_123';
CREATE USER IF NOT EXISTS 'tms_notif_user'@'%'
    IDENTIFIED BY 'notif_pass_123';

GRANT ALL PRIVILEGES ON tms_auth_db.*         TO 'tms_auth_user'@'%';
GRANT ALL PRIVILEGES ON tms_timesheet_db.*    TO 'tms_ts_user'@'%';
GRANT ALL PRIVILEGES ON tms_leave_db.*        TO 'tms_leave_user'@'%';
GRANT ALL PRIVILEGES ON tms_admin_db.*        TO 'tms_admin_user'@'%';
GRANT ALL PRIVILEGES ON tms_notification_db.* TO 'tms_notif_user'@'%';

FLUSH PRIVILEGES;