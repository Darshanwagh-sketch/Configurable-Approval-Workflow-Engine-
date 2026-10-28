-- Seed Data for Configurable Approval Workflow Engine
-- Password for all seed users is 'password123' (admin has 'admin123'), BCrypt hashed:
-- 'password123' -> $2a$10$w09uV9H2uWq/O6Y5e17uQOh8m3c5JvQ/gV4V0m4u5K9xGvF4zRvy6
-- 'admin123'    -> $2a$10$wK1Gv5KzVw/A5q8oF5nZweaB.vLg7m7nK8P1X2Y3Z4a5b6c7d8e9f

-- Initial Development Users
INSERT INTO users (username, password_hash, role, enabled, created_at)
SELECT 'requester1', '$2a$10$w09uV9H2uWq/O6Y5e17uQOh8m3c5JvQ/gV4V0m4u5K9xGvF4zRvy6', 'REQUESTER', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'requester1');

INSERT INTO users (username, password_hash, role, enabled, created_at)
SELECT 'approver1', '$2a$10$w09uV9H2uWq/O6Y5e17uQOh8m3c5JvQ/gV4V0m4u5K9xGvF4zRvy6', 'APPROVER', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'approver1');

INSERT INTO users (username, password_hash, role, enabled, created_at)
SELECT 'admin', '$2a$10$wK1Gv5KzVw/A5q8oF5nZweaB.vLg7m7nK8P1X2Y3Z4a5b6c7d8e9f', 'ADMIN', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

-- Dynamic Workflow Configuration: LEAVE (2 steps)
INSERT INTO approval_steps (request_type, step_order, role, workflow_version, active)
SELECT 'LEAVE', 1, 'APPROVER', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM approval_steps WHERE request_type = 'LEAVE' AND step_order = 1);

INSERT INTO approval_steps (request_type, step_order, role, workflow_version, active)
SELECT 'LEAVE', 2, 'ADMIN', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM approval_steps WHERE request_type = 'LEAVE' AND step_order = 2);

-- Dynamic Workflow Configuration: EXPENSE (2 steps)
INSERT INTO approval_steps (request_type, step_order, role, workflow_version, active)
SELECT 'EXPENSE', 1, 'APPROVER', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM approval_steps WHERE request_type = 'EXPENSE' AND step_order = 1);

INSERT INTO approval_steps (request_type, step_order, role, workflow_version, active)
SELECT 'EXPENSE', 2, 'ADMIN', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM approval_steps WHERE request_type = 'EXPENSE' AND step_order = 2);
