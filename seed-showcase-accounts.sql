-- =========================================================================
-- AGENCY OS — FULL SHOWCASE & DEMO SEED SCRIPT (8 USERS)
-- Always resets demo workspace and tenant data relative to current time (NOW())
-- =========================================================================

-- 1. PUBLIC SCHEMA: Clean & Seed Demo Users and Showcase Workspace
SET search_path TO public;

GRANT ALL ON SCHEMA public TO PUBLIC;
GRANT ALL ON ALL TABLES IN SCHEMA public TO PUBLIC;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO PUBLIC;

DELETE FROM public.workspaces WHERE tenant_id = 'tenant_apex_digital_demo';

DELETE FROM public.app_users 
WHERE username IN (
    'alex_owner', 'marcus_admin', 'rachel_admin',
    'sarah_member', 'liam_member', 'james_member',
    'david_client', 'elena_client'
) OR keycloak_id IN (
    'fc8c99b7-3f29-43f3-917a-3f24f0da41c4', '2656c2b1-c49b-41cb-93d6-006468376408',
    'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6',
    '4c223473-fc1e-4686-813b-dd50a87fa654', '1319b3a7-11a7-4410-b0e9-86e8bef74e77',
    'd578ed5e-3c18-473e-af41-3d7a4e51c403', 'a0989c21-8c23-494d-a5f7-c5ad7c51e4df'
);

INSERT INTO public.app_users (id, keycloak_id, username, email, first_name, last_name, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'fc8c99b7-3f29-43f3-917a-3f24f0da41c4', 'alex_owner', 'alex.owner@agency-os.dev', 'Alex', 'Vance', NOW() - INTERVAL '30 days', NOW()),
    ('22222222-2222-2222-2222-222222222222', '2656c2b1-c49b-41cb-93d6-006468376408', 'marcus_admin', 'marcus.admin@agency-os.dev', 'Marcus', 'Wright', NOW() - INTERVAL '30 days', NOW()),
    ('33333333-3333-3333-3333-333333333333', 'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d', 'rachel_admin', 'rachel.admin@agency-os.dev', 'Rachel', 'Adams', NOW() - INTERVAL '30 days', NOW()),
    ('44444444-4444-4444-4444-444444444444', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 'sarah_member', 'sarah.member@agency-os.dev', 'Sarah', 'Chen', NOW() - INTERVAL '30 days', NOW()),
    ('55555555-5555-5555-5555-555555555555', '4c223473-fc1e-4686-813b-dd50a87fa654', 'liam_member', 'liam.member@agency-os.dev', 'Liam', 'Rodriguez', NOW() - INTERVAL '30 days', NOW()),
    ('66666666-6666-6666-6666-666666666666', '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 'james_member', 'james.member@agency-os.dev', 'James', 'Wilson', NOW() - INTERVAL '30 days', NOW()),
    ('77777777-7777-7777-7777-777777777777', 'd578ed5e-3c18-473e-af41-3d7a4e51c403', 'david_client', 'david.client@cybernetic.co', 'David', 'Miller', NOW() - INTERVAL '30 days', NOW()),
    ('88888888-8888-8888-8888-888888888888', 'a0989c21-8c23-494d-a5f7-c5ad7c51e4df', 'elena_client', 'elena.client@velocitylabs.io', 'Elena', 'Rostova', NOW() - INTERVAL '30 days', NOW());

INSERT INTO public.workspaces (id, name, tenant_id, contact_email, is_active, created_at, updated_at)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Apex Digital Studio (Demo)', 'tenant_apex_digital_demo', 'contact@apexdigital.demo', true, NOW() - INTERVAL '30 days', NOW());

INSERT INTO public.user_workspaces (user_id, workspace_id, role)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'OWNER'),
    ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN'),
    ('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN'),
    ('44444444-4444-4444-4444-444444444444', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
    ('55555555-5555-5555-5555-555555555555', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
    ('66666666-6666-6666-6666-666666666666', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'MEMBER'),
    ('77777777-7777-7777-7777-777777777777', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CLIENT'),
    ('88888888-8888-8888-8888-888888888888', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'CLIENT');


-- 2. TENANT SCHEMA: Clean Previous Records
CREATE SCHEMA IF NOT EXISTS tenant_apex_digital_demo;

DELETE FROM tenant_apex_digital_demo.active_timers;
DELETE FROM tenant_apex_digital_demo.time_entries;
DELETE FROM tenant_apex_digital_demo.task_assignees;
DELETE FROM tenant_apex_digital_demo.tasks;
DELETE FROM tenant_apex_digital_demo.projects;
DELETE FROM tenant_apex_digital_demo.client_users;
DELETE FROM tenant_apex_digital_demo.invoices;
DELETE FROM tenant_apex_digital_demo.clients;


-- 3. TENANT SCHEMA: Seed Standard Representative Data with Fresh Dates Relative to NOW()

-- Clients
INSERT INTO tenant_apex_digital_demo.clients (id, name, email, status, is_active, created_at, updated_at)
VALUES 
    ('ca000000-0000-0000-0000-000000000001', 'Cybernetic Co.', 'billing@cybernetic.co', 'ACTIVE', true, NOW() - INTERVAL '30 days', NOW()),
    ('ca000000-0000-0000-0000-000000000002', 'Velocity Labs', 'accounts@velocitylabs.io', 'ACTIVE', true, NOW() - INTERVAL '25 days', NOW()),
    ('ca000000-0000-0000-0000-000000000003', 'Apex FinTech Solutions', 'finance@apexfintech.com', 'PROSPECT', true, NOW() - INTERVAL '10 days', NOW());

-- Client Portal Users Mapping
INSERT INTO tenant_apex_digital_demo.client_users (user_id, client_id, created_at, updated_at)
VALUES 
    ('d578ed5e-3c18-473e-af41-3d7a4e51c403', 'ca000000-0000-0000-0000-000000000001', NOW() - INTERVAL '30 days', NOW()),
    ('a0989c21-8c23-494d-a5f7-c5ad7c51e4df', 'ca000000-0000-0000-0000-000000000002', NOW() - INTERVAL '25 days', NOW());

-- Projects
INSERT INTO tenant_apex_digital_demo.projects (id, client_id, name, description, budget, status, billing_rate, is_active, created_at, updated_at)
VALUES 
    ('ba000000-0000-0000-0000-000000000001', 'ca000000-0000-0000-0000-000000000001', 'AI Cloud Migration',
     'Complete infrastructure migration to distributed cloud with automated telemetry.', 45000.00, 'IN_PROGRESS', 175.00, true, NOW() - INTERVAL '20 days', NOW()),
    ('ba000000-0000-0000-0000-000000000002', 'ca000000-0000-0000-0000-000000000001', 'Mobile App Redesign',
     'Cross-platform mobile overhaul with biometric security and offline sync.', 28000.00, 'PLANNING', 150.00, true, NOW() - INTERVAL '15 days', NOW()),
    ('ba000000-0000-0000-0000-000000000003', 'ca000000-0000-0000-0000-000000000002', 'Design System & Brand Portal',
     'Multi-brand Figma token library and accessible Angular component kit.', 18500.00, 'IN_PROGRESS', 160.00, true, NOW() - INTERVAL '18 days', NOW()),
    ('ba000000-0000-0000-0000-000000000004', 'ca000000-0000-0000-0000-000000000002', 'Telemetry & Metrics Pipeline',
     'Prometheus and Grafana real-time metrics dashboards.', 12000.00, 'DELIVERED', 160.00, true, NOW() - INTERVAL '25 days', NOW());

-- Tasks (Clean distribution of active, review/overdue, due-soon, and completed tasks)
INSERT INTO tenant_apex_digital_demo.tasks (id, project_id, title, description, start_date, due_date, estimated_minutes, priority, status, created_at, updated_at)
VALUES 
    ('da000000-0000-0000-0000-000000000001', 'ba000000-0000-0000-0000-000000000001',
     'Multi-Region Kubernetes Deployment', 'Set up Terraform IaC and ArgoCD sync.',
     NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', 720, 'URGENT', 'IN_PROGRESS', NOW() - INTERVAL '3 days', NOW()),
    ('da000000-0000-0000-0000-000000000002', 'ba000000-0000-0000-0000-000000000001',
     'Zero-Trust JWT & OIDC Integration', 'Connect Spring Security resource server with Keycloak JWKS.',
     NOW() - INTERVAL '7 days', NOW() - INTERVAL '1 day', 480, 'HIGH', 'REVIEW', NOW() - INTERVAL '7 days', NOW()),
    ('da000000-0000-0000-0000-000000000003', 'ba000000-0000-0000-0000-000000000001',
     'PostgreSQL Schema Migration Pipeline', 'Automate tenant schema DDL migrations using Flyway.',
     NOW() - INTERVAL '10 days', NOW() - INTERVAL '6 days', 360, 'MEDIUM', 'DONE', NOW() - INTERVAL '10 days', NOW()),
    ('da000000-0000-0000-0000-000000000004', 'ba000000-0000-0000-0000-000000000003',
     'Design Token Architecture in Figma', 'Standardize spacing, typography, and dark-theme color tokens.',
     NOW() - INTERVAL '2 days', NOW() + INTERVAL '3 days', 300, 'HIGH', 'IN_PROGRESS', NOW() - INTERVAL '2 days', NOW()),
    ('da000000-0000-0000-0000-000000000005', 'ba000000-0000-0000-0000-000000000003',
     'Tailwind v4 Semantic Theme Mapping', 'Implement `@theme` utilities in Angular styles.',
     NOW() - INTERVAL '1 day', NOW() + INTERVAL '1 day', 240, 'MEDIUM', 'TODO', NOW() - INTERVAL '1 day', NOW()),
    ('da000000-0000-0000-0000-000000000006', 'ba000000-0000-0000-0000-000000000002',
     'Figma Prototype Review with Stakeholders', 'Validate user flow for biometric authentication.',
     NOW() + INTERVAL '1 day', NOW() + INTERVAL '5 days', 240, 'LOW', 'TODO', NOW(), NOW()),
    ('da000000-0000-0000-0000-000000000007', 'ba000000-0000-0000-0000-000000000004',
     'Prometheus Exporters & AlertManager', 'Configure alert rules for p99 latency spikes and error budgets.',
     NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', 360, 'HIGH', 'DONE', NOW() - INTERVAL '5 days', NOW());

-- Task Assignees
INSERT INTO tenant_apex_digital_demo.task_assignees (task_id, user_id)
VALUES 
    ('da000000-0000-0000-0000-000000000001', '1319b3a7-11a7-4410-b0e9-86e8bef74e77'),
    ('da000000-0000-0000-0000-000000000001', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'),
    ('da000000-0000-0000-0000-000000000002', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'),
    ('da000000-0000-0000-0000-000000000002', '2656c2b1-c49b-41cb-93d6-006468376408'),
    ('da000000-0000-0000-0000-000000000003', 'fc8c99b7-3f29-43f3-917a-3f24f0da41c4'),
    ('da000000-0000-0000-0000-000000000004', '4c223473-fc1e-4686-813b-dd50a87fa654'),
    ('da000000-0000-0000-0000-000000000004', 'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d'),
    ('da000000-0000-0000-0000-000000000005', '4c223473-fc1e-4686-813b-dd50a87fa654'),
    ('da000000-0000-0000-0000-000000000005', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6'),
    ('da000000-0000-0000-0000-000000000006', '2656c2b1-c49b-41cb-93d6-006468376408'),
    ('da000000-0000-0000-0000-000000000007', '1319b3a7-11a7-4410-b0e9-86e8bef74e77');

-- Invoices
INSERT INTO tenant_apex_digital_demo.invoices (id, client_id, total_amount, status, created_at, updated_at)
VALUES 
    ('ea000000-0000-0000-0000-000000000001', 'ca000000-0000-0000-0000-000000000001', 4200.00, 'PAID', NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
    ('ea000000-0000-0000-0000-000000000002', 'ca000000-0000-0000-0000-000000000001', 3150.00, 'SENT', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('ea000000-0000-0000-0000-000000000003', 'ca000000-0000-0000-0000-000000000002', 4800.00, 'PAID', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('ea000000-0000-0000-0000-000000000004', 'ca000000-0000-0000-0000-000000000002', 2560.00, 'DRAFT', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- Time Entries
INSERT INTO tenant_apex_digital_demo.time_entries (id, task_id, user_id, duration_minutes, is_billable, invoice_id, created_at, updated_at)
VALUES 
    ('fa000000-0000-0000-0000-000000000001', 'da000000-0000-0000-0000-000000000003', 'fc8c99b7-3f29-43f3-917a-3f24f0da41c4', 360, true, 'ea000000-0000-0000-0000-000000000001', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
    ('fa000000-0000-0000-0000-000000000006', 'da000000-0000-0000-0000-000000000002', '2656c2b1-c49b-41cb-93d6-006468376408', 180, true, NULL, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('fa000000-0000-0000-0000-000000000009', 'da000000-0000-0000-0000-000000000004', 'd26e9d31-0e5f-4aff-a9b6-1da7b53d013d', 150, true, 'ea000000-0000-0000-0000-000000000003', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('fa000000-0000-0000-0000-000000000002', 'da000000-0000-0000-0000-000000000002', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 480, true, 'ea000000-0000-0000-0000-000000000002', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('fa000000-0000-0000-0000-000000000003', 'da000000-0000-0000-0000-000000000001', '8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 210, true, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('fa000000-0000-0000-0000-000000000004', 'da000000-0000-0000-0000-000000000004', '4c223473-fc1e-4686-813b-dd50a87fa654', 180, true, 'ea000000-0000-0000-0000-000000000003', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('fa000000-0000-0000-0000-000000000005', 'da000000-0000-0000-0000-000000000004', '4c223473-fc1e-4686-813b-dd50a87fa654', 120, true, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('fa000000-0000-0000-0000-000000000007', 'da000000-0000-0000-0000-000000000007', '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 360, true, 'ea000000-0000-0000-0000-000000000003', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('fa000000-0000-0000-0000-000000000008', 'da000000-0000-0000-0000-000000000001', '1319b3a7-11a7-4410-b0e9-86e8bef74e77', 240, true, NULL, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- Active Timers (Sarah running stopwatch at 1h 24m, Liam paused stopwatch at 45m)
INSERT INTO tenant_apex_digital_demo.active_timers (user_id, task_id, start_time, is_paused, accumulated_seconds, last_resume_timestamp)
VALUES 
    ('8e0ad35e-1f73-47c1-afd4-dbcae86a3bb6', 'da000000-0000-0000-0000-000000000001',
     NOW() - INTERVAL '1 hour 24 minutes', false, 0, NOW() - INTERVAL '1 hour 24 minutes'),
    ('4c223473-fc1e-4686-813b-dd50a87fa654', 'da000000-0000-0000-0000-000000000004',
     NOW() - INTERVAL '45 minutes', true, 2700, NULL);

-- Grant privileges
GRANT ALL ON SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL TABLES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
GRANT ALL ON ALL SEQUENCES IN SCHEMA tenant_apex_digital_demo TO PUBLIC;
