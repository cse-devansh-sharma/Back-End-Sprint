CREATE DATABASE IF NOT EXISTS tms_auth_db;
CREATE DATABASE IF NOT EXISTS tms_admin_db;
CREATE DATABASE IF NOT EXISTS tms_leave_db;
CREATE DATABASE IF NOT EXISTS tms_timesheet_db;
CREATE DATABASE IF NOT EXISTS tms_notification_db;

GRANT ALL PRIVILEGES ON tms_auth_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON tms_admin_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON tms_leave_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON tms_timesheet_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON tms_notification_db.* TO 'root'@'%';
FLUSH PRIVILEGES;
