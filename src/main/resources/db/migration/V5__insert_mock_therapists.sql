-- Insert 30 diverse mock therapists for testing and demonstration

INSERT INTO therapists (therapist_id, account_id, full_name, specialization, country, years_experience, about_me, rating_avg, license_url, gender, is_lgbtq_allied, communication_style, treated_challenges)
VALUES
-- 1-5: Female therapists, diverse specializations
('aa84aebd-0ec3-38c6-fc8e-8edaf08ba7dc'::uuid, 'aa84aebd-0ec3-38c6-fc8e-8edaf08ba7dc'::uuid, 'Dr. Chau Vo', 'Cognitive Behavioral Therapy', 'United States', 12, 'Specialized in cognitive behavioral therapy techniques with 12 years of clinical experience.', 4.85, 'https://license.example.com/sarah-johnson', 'Female', true, 'empathetic', ARRAY['anxiety', 'panic', 'stress']),
('83c9dae2-a937-6866-ec4f-373a8517173e'::uuid, '83c9dae2-a937-6866-ec4f-373a8517173e'::uuid, 'Dr. Duy Hoang', 'Adolescent Anxiety Support', 'Canada', 15, 'Experienced with depression management and antidepressant counseling. Holistic approach to mental health.', 4.72, 'https://license.example.com/maria-chen', 'Female', true, 'supportive', ARRAY['depression', 'anxiety', 'burnout']),
('6555ccce-c50b-d19e-41b0-93300f4eb9d1'::uuid, '6555ccce-c50b-d19e-41b0-93300f4eb9d1'::uuid, 'Dr. Ha Tran', 'Family Counseling', 'Nigeria', 10, 'Specializing in trauma processing and EMDR therapy. Compassionate approach to healing.', 4.88, 'https://license.example.com/amara-okafor', 'Female', true, 'directive', ARRAY['trauma', 'ptsd', 'anxiety']),
('55c527a2-d135-9418-355d-985b9514e160'::uuid, '55c527a2-d135-9418-355d-985b9514e160'::uuid, 'Dr. Khoa Dang', 'Trauma-Informed Care', 'Italy', 18, 'Expert in couples therapy and communication skills. Deep understanding of relational dynamics.', 4.79, 'https://license.example.com/elena-rossi', 'Female', false, 'analytical', ARRAY['relationships', 'communication', 'anxiety']),
('1ee83ad7-1b39-9121-3401-fd648c4460b4'::uuid, '1ee83ad7-1b39-9121-3401-fd648c4460b4'::uuid, 'Dr. Lam Phan', 'Mindfulness-Based Therapy', 'Australia', 14, 'Specialized treatment for eating disorders and body image issues. Evidence-based methods.', 4.81, 'https://license.example.com/patricia-williams', 'Female', true, 'supportive', ARRAY['eating_disorders', 'anxiety', 'self_esteem']),

-- 6-10: Male therapists, diverse specializations
('4f7a4732-e1d4-047b-0520-71cd78166f0b'::uuid, '4f7a4732-e1d4-047b-0520-71cd78166f0b'::uuid, 'Dr. Mai Le', 'School Stress Management', 'United Kingdom', 16, 'Addiction specialist with recovery-focused approach. 16 years in rehabilitation counseling.', 4.76, 'https://license.example.com/james-mitchell', 'Male', true, 'directive', ARRAY['addiction', 'substance_abuse', 'anxiety']),
('80fa1ca8-b13f-8c05-7c4e-c3063c8c64c6'::uuid, '80fa1ca8-b13f-8c05-7c4e-c3063c8c64c6'::uuid, 'Dr. Nhi Bui', 'Cognitive Behavioral Therapy', 'Japan', 11, 'Career transition and workplace stress specialist. Mindfulness-based approach to life planning.', 4.83, 'https://license.example.com/kenji-tanaka', 'Male', false, 'analytical', ARRAY['stress', 'career_issues', 'burnout']),
('d628d407-0cfe-3aa7-4be7-5e36fe310e54'::uuid, 'd628d407-0cfe-3aa7-4be7-5e36fe310e54'::uuid, 'Dr. Phat Vu', 'Adolescent Anxiety Support', 'Saudi Arabia', 13, 'Compassionate grief counselor with multicultural sensitivity. Specializing in bereavement support.', 4.89, 'https://license.example.com/mohammad-hassan', 'Male', true, 'empathetic', ARRAY['grief', 'loss', 'depression']),
('b77dcce6-f77f-ced4-d966-baa3c1b14bbb'::uuid, 'b77dcce6-f77f-ced4-d966-baa3c1b14bbb'::uuid, 'Dr. Thu Pham', 'Family Counseling', 'Spain', 17, 'Family systems expert with decades of experience in multi-generational healing.', 4.74, 'https://license.example.com/carlos-rodriguez', 'Male', false, 'supportive', ARRAY['relationships', 'family_issues', 'anxiety']),
('34c29aa5-2136-c75e-39d2-01d423ece3e2'::uuid, '34c29aa5-2136-c75e-39d2-01d423ece3e2'::uuid, 'Dr. Bao Nguyen', 'Trauma-Informed Care', 'India', 12, 'Specialized in Exposure and Response Prevention for OCD. Focused, evidence-based techniques.', 4.87, 'https://license.example.com/rajesh-kumar', 'Male', true, 'analytical', ARRAY['anxiety', 'ocd', 'panic']),

-- 11-15: Non-binary and diverse gender expressions
('f446f8b7-fb1f-9f5a-f03c-215d83d5d7ad'::uuid, 'f446f8b7-fb1f-9f5a-f03c-215d83d5d7ad'::uuid, 'Dr. Chau Vo', 'Mindfulness-Based Therapy', 'United States', 9, 'Affirming therapist specializing in identity and coming-out support for LGBTQ+ individuals.', 4.91, 'https://license.example.com/alex-morgan', 'Non-binary', true, 'empathetic', ARRAY['lgbtq_identity', 'anxiety', 'depression']),
('552148ef-382a-0586-003d-b2b6c0589252'::uuid, '552148ef-382a-0586-003d-b2b6c0589252'::uuid, 'Dr. Duy Hoang', 'School Stress Management', 'Canada', 8, 'Neurodivergent-affirming therapist. Expert in supporting adults on the autism spectrum.', 4.85, 'https://license.example.com/sam-knight', 'Non-binary', true, 'analytical', ARRAY['autism', 'anxiety', 'social_anxiety']),
('43711408-3919-1fdf-f6a5-99bae375e283'::uuid, '43711408-3919-1fdf-f6a5-99bae375e283'::uuid, 'Dr. Ha Tran', 'Cognitive Behavioral Therapy', 'Singapore', 10, 'Integrating traditional wellness practices with modern therapy. Mindfulness and meditation focus.', 4.80, 'https://license.example.com/priya-nair', 'Female', true, 'empathetic', ARRAY['stress', 'anxiety', 'burnout']),
('a80cba36-329a-c14e-2e5a-92b37955badf'::uuid, 'a80cba36-329a-c14e-2e5a-92b37955badf'::uuid, 'Dr. Khoa Dang', 'Adolescent Anxiety Support', 'United Kingdom', 11, 'Specializing in teen anxiety, depression, and identity issues. Worked in school settings.', 4.78, 'https://license.example.com/jordan-ellis', 'Non-binary', true, 'supportive', ARRAY['anxiety', 'depression', 'identity_issues']),
('78daf827-ac07-2a37-6806-64a950071e12'::uuid, '78daf827-ac07-2a37-6806-64a950071e12'::uuid, 'Dr. Lam Phan', 'Family Counseling', 'Iran', 14, 'Culturally responsive therapy for refugees and immigrants. Multilingual practice.', 4.82, 'https://license.example.com/leila-rezaei', 'Female', false, 'empathetic', ARRAY['cultural_adjustment', 'trauma', 'anxiety']),

-- 16-20: More male and international therapists
('73f328c4-b42f-51fd-812d-017acb0bfb7a'::uuid, '73f328c4-b42f-51fd-812d-017acb0bfb7a'::uuid, 'Dr. Mai Le', 'Trauma-Informed Care', 'Ireland', 13, 'Behavioral specialist in anger and emotional regulation. Proven track record in conflict resolution.', 4.77, 'https://license.example.com/david-obrien', 'Male', true, 'directive', ARRAY['anger', 'emotional_regulation', 'relationships']),
('c41cb427-3f30-f7db-e55d-78b543fcd713'::uuid, 'c41cb427-3f30-f7db-e55d-78b543fcd713'::uuid, 'Dr. Nhi Bui', 'Mindfulness-Based Therapy', 'Japan', 10, 'Works with high-achievers experiencing burnout. Specializes in perfectionism patterns.', 4.84, 'https://license.example.com/yuki-sato', 'Female', false, 'analytical', ARRAY['burnout', 'stress', 'anxiety']),
('f2052cf5-1928-15dc-8ae3-1a28a1373989'::uuid, 'f2052cf5-1928-15dc-8ae3-1a28a1373989'::uuid, 'Dr. Phat Vu', 'School Stress Management', 'Italy', 19, 'Expert in attachment theory and relational dynamics. Decades of couples and individual work.', 4.86, 'https://license.example.com/marco-fontana', 'Male', true, 'empathetic', ARRAY['relationships', 'anxiety', 'depression']),
('d0ea8501-ead8-41da-58ef-6fbe0e47656d'::uuid, 'd0ea8501-ead8-41da-58ef-6fbe0e47656d'::uuid, 'Dr. Thu Pham', 'Cognitive Behavioral Therapy', 'Kuwait', 12, 'Specialized in women''s mental health, reproductive issues, and trauma recovery.', 4.88, 'https://license.example.com/fatima-almakki', 'Female', true, 'supportive', ARRAY['trauma', 'depression', 'anxiety']),
('6e4ae57a-a740-85db-7285-22571d2e10fd'::uuid, '6e4ae57a-a740-85db-7285-22571d2e10fd'::uuid, 'Dr. Bao Nguyen', 'Adolescent Anxiety Support', 'Australia', 11, 'Mental performance coach for athletes. Specializes in sports-related anxiety and pressure.', 4.75, 'https://license.example.com/bruce-thompson', 'Male', false, 'directive', ARRAY['anxiety', 'stress', 'performance_issues']),

-- 21-25: More diverse backgrounds
('1b3c5faa-8c05-0cba-c50f-49303a0c77b0'::uuid, '1b3c5faa-8c05-0cba-c50f-49303a0c77b0'::uuid, 'Dr. Chau Vo', 'Family Counseling', 'Russia', 15, 'Trauma specialist with extensive experience in complex trauma and dissociation.', 4.90, 'https://license.example.com/natasha-volkov', 'Female', true, 'analytical', ARRAY['trauma', 'ptsd', 'dissociation']),
('5e1342d0-3ffa-3c1a-6f2e-ce107884278f'::uuid, '5e1342d0-3ffa-3c1a-6f2e-ce107884278f'::uuid, 'Dr. Duy Hoang', 'Trauma-Informed Care', 'South Africa', 10, 'Life purpose and resilience coach. Helping clients find meaning and direction.', 4.79, 'https://license.example.com/thabo-mkhize', 'Male', true, 'empathetic', ARRAY['stress', 'depression', 'burnout']),
('95dc294e-8f2c-08ad-2320-955d32d7bd09'::uuid, '95dc294e-8f2c-08ad-2320-955d32d7bd09'::uuid, 'Dr. Ha Tran', 'Mindfulness-Based Therapy', 'Greece', 12, 'Body-focused therapy and somatic experiencing. Connecting mind and body for healing.', 4.83, 'https://license.example.com/sophia-papadopoulos', 'Female', false, 'empathetic', ARRAY['anxiety', 'trauma', 'stress']),
('b9de030c-4adb-4ac1-5e73-d97db1fa6c98'::uuid, 'b9de030c-4adb-4ac1-5e73-d97db1fa6c98'::uuid, 'Dr. Khoa Dang', 'School Stress Management', 'Sweden', 9, 'Cognitive behavioral therapy for insomnia (CBT-I). Sleep improvement specialist.', 4.81, 'https://license.example.com/henrik-bergstrom', 'Male', false, 'analytical', ARRAY['anxiety', 'insomnia', 'stress']),
('d95cd44e-543e-7d23-2a7f-76c1cf5eea39'::uuid, 'd95cd44e-543e-7d23-2a7f-76c1cf5eea39'::uuid, 'Dr. Lam Phan', 'Cognitive Behavioral Therapy', 'Brazil', 11, 'Specialist in health anxiety and somatic symptom disorders. Reassurance and symptom management.', 4.77, 'https://license.example.com/amelia-santos', 'Female', true, 'supportive', ARRAY['anxiety', 'health_anxiety', 'stress']),

-- 26-30: Final diverse selections
('138bf1e0-a096-a522-fae7-eae964290a81'::uuid, '138bf1e0-a096-a522-fae7-eae964290a81'::uuid, 'Dr. Mai Le', 'Adolescent Anxiety Support', 'Ghana', 8, 'Building confidence and self-esteem in individuals. Positive psychology approach.', 4.80, 'https://license.example.com/kwame-asante', 'Male', true, 'supportive', ARRAY['self_esteem', 'anxiety', 'depression']),
('cec1c074-133c-1a56-1ffa-8e67f564b43f'::uuid, 'cec1c074-133c-1a56-1ffa-8e67f564b43f'::uuid, 'Dr. Nhi Bui', 'Family Counseling', 'Norway', 13, 'Helping patients manage psychological aspects of chronic pain and medical conditions.', 4.82, 'https://license.example.com/lisa-bergman', 'Female', false, 'empathetic', ARRAY['chronic_pain', 'depression', 'anxiety']),
('59fa09cb-22fb-f2c5-2903-1ae561badcaf'::uuid, '59fa09cb-22fb-f2c5-2903-1ae561badcaf'::uuid, 'Dr. Phat Vu', 'Trauma-Informed Care', 'Iran', 10, 'Specialized in social anxiety disorder and agoraphobia. Exposure therapy and skills training.', 4.85, 'https://license.example.com/amir-behzadi', 'Male', true, 'analytical', ARRAY['social_anxiety', 'anxiety', 'panic']),
('327d3084-0b7f-420f-1a4b-f158dd4de2c0'::uuid, '327d3084-0b7f-420f-1a4b-f158dd4de2c0'::uuid, 'Dr. Thu Pham', 'Mindfulness-Based Therapy', 'Mexico', 14, 'Crisis management and suicide prevention. Dialectical Behavior Therapy (DBT) specialist.', 4.92, 'https://license.example.com/rosa-mendez', 'Female', true, 'directive', ARRAY['self_harm', 'suicidality', 'depression']),
('ede12bcd-9193-b864-a71b-9f3c59c91d80'::uuid, 'ede12bcd-9193-b864-a71b-9f3c59c91d80'::uuid, 'Dr. Bao Nguyen', 'School Stress Management', 'United States', 9, 'Helping clients navigate major life changes and transitions with resilience and clarity.', 4.78, 'https://license.example.com/ethan-whitmore', 'Male', true, 'empathetic', ARRAY['stress', 'anxiety', 'depression'])
ON CONFLICT DO NOTHING;

-- Insert many weekly templates for seeded therapists.
-- Generates deterministic UUIDs from therapist/day/time so reruns are safe.
WITH seeded_therapists AS (
	SELECT therapist_id
	FROM therapists
	WHERE therapist_id::text LIKE '550e8400-e29b-41d4-a716-4466554400%'
),
template_windows(day_of_week, start_time, end_time) AS (
	VALUES
		('MONDAY', '09:00', '12:00'),
		('MONDAY', '13:00', '17:00'),
		('TUESDAY', '09:00', '12:00'),
		('TUESDAY', '13:00', '17:00'),
		('WEDNESDAY', '09:00', '12:00'),
		('WEDNESDAY', '13:00', '17:00'),
		('THURSDAY', '09:00', '12:00'),
		('THURSDAY', '13:00', '17:00'),
		('FRIDAY', '09:00', '12:00'),
		('FRIDAY', '13:00', '17:00'),
		('SATURDAY', '09:00', '12:00'),
		('SUNDAY', '16:00', '18:00')
)
INSERT INTO weekly_templates (template_id, therapist_id, day_of_week, start_time, end_time, is_active)
SELECT
	(
		SUBSTRING(h.hash_value, 1, 8) || '-' ||
		SUBSTRING(h.hash_value, 9, 4) || '-' ||
		SUBSTRING(h.hash_value, 13, 4) || '-' ||
		SUBSTRING(h.hash_value, 17, 4) || '-' ||
		SUBSTRING(h.hash_value, 21, 12)
	)::uuid AS template_id,
	st.therapist_id,
	tw.day_of_week,
	tw.start_time::time,
	tw.end_time::time,
	TRUE
FROM seeded_therapists st
CROSS JOIN template_windows tw
CROSS JOIN LATERAL (
	SELECT md5(st.therapist_id::text || '|' || tw.day_of_week || '|' || tw.start_time || '|' || tw.end_time) AS hash_value
) h
ON CONFLICT DO NOTHING;
