-- Diversify mock therapist full names seeded in V5.
-- V5 reused 10 names across 30 rows; this migration assigns 30 unique
-- Vietnamese full names while preserving each therapist's recorded gender.

UPDATE therapists SET full_name = 'Dr. Linh Nguyen'  WHERE therapist_id = 'aa84aebd-0ec3-38c6-fc8e-8edaf08ba7dc'::uuid;
UPDATE therapists SET full_name = 'Dr. Anh Tran'     WHERE therapist_id = '83c9dae2-a937-6866-ec4f-373a8517173e'::uuid;
UPDATE therapists SET full_name = 'Dr. Ngoc Le'      WHERE therapist_id = '6555ccce-c50b-d19e-41b0-93300f4eb9d1'::uuid;
UPDATE therapists SET full_name = 'Dr. Hanh Pham'    WHERE therapist_id = '55c527a2-d135-9418-355d-985b9514e160'::uuid;
UPDATE therapists SET full_name = 'Dr. Trang Hoang'  WHERE therapist_id = '1ee83ad7-1b39-9121-3401-fd648c4460b4'::uuid;
UPDATE therapists SET full_name = 'Dr. Tuan Phan'    WHERE therapist_id = '4f7a4732-e1d4-047b-0520-71cd78166f0b'::uuid;
UPDATE therapists SET full_name = 'Dr. Hung Vu'      WHERE therapist_id = '80fa1ca8-b13f-8c05-7c4e-c3063c8c64c6'::uuid;
UPDATE therapists SET full_name = 'Dr. Minh Vo'      WHERE therapist_id = 'd628d407-0cfe-3aa7-4be7-5e36fe310e54'::uuid;
UPDATE therapists SET full_name = 'Dr. Long Dang'    WHERE therapist_id = 'b77dcce6-f77f-ced4-d966-baa3c1b14bbb'::uuid;
UPDATE therapists SET full_name = 'Dr. Tien Bui'     WHERE therapist_id = '34c29aa5-2136-c75e-39d2-01d423ece3e2'::uuid;
UPDATE therapists SET full_name = 'Dr. An Do'        WHERE therapist_id = 'f446f8b7-fb1f-9f5a-f03c-215d83d5d7ad'::uuid;
UPDATE therapists SET full_name = 'Dr. Khanh Ho'     WHERE therapist_id = '552148ef-382a-0586-003d-b2b6c0589252'::uuid;
UPDATE therapists SET full_name = 'Dr. Hoa Ngo'      WHERE therapist_id = '43711408-3919-1fdf-f6a5-99bae375e283'::uuid;
UPDATE therapists SET full_name = 'Dr. Bao Duong'    WHERE therapist_id = 'a80cba36-329a-c14e-2e5a-92b37955badf'::uuid;
UPDATE therapists SET full_name = 'Dr. Thuy Ly'      WHERE therapist_id = '78daf827-ac07-2a37-6806-64a950071e12'::uuid;
UPDATE therapists SET full_name = 'Dr. Dung Truong'  WHERE therapist_id = '73f328c4-b42f-51fd-812d-017acb0bfb7a'::uuid;
UPDATE therapists SET full_name = 'Dr. Tam Mai'      WHERE therapist_id = 'c41cb427-3f30-f7db-e55d-78b543fcd713'::uuid;
UPDATE therapists SET full_name = 'Dr. Quang Dinh'   WHERE therapist_id = 'f2052cf5-1928-15dc-8ae3-1a28a1373989'::uuid;
UPDATE therapists SET full_name = 'Dr. Yen Cao'      WHERE therapist_id = 'd0ea8501-ead8-41da-58ef-6fbe0e47656d'::uuid;
UPDATE therapists SET full_name = 'Dr. Hieu Lam'     WHERE therapist_id = '6e4ae57a-a740-85db-7285-22571d2e10fd'::uuid;
UPDATE therapists SET full_name = 'Dr. Lan Trinh'    WHERE therapist_id = '1b3c5faa-8c05-0cba-c50f-49303a0c77b0'::uuid;
UPDATE therapists SET full_name = 'Dr. Phuc Luu'     WHERE therapist_id = '5e1342d0-3ffa-3c1a-6f2e-ce107884278f'::uuid;
UPDATE therapists SET full_name = 'Dr. Hong Chu'     WHERE therapist_id = '95dc294e-8f2c-08ad-2320-955d32d7bd09'::uuid;
UPDATE therapists SET full_name = 'Dr. Nam Quach'    WHERE therapist_id = 'b9de030c-4adb-4ac1-5e73-d97db1fa6c98'::uuid;
UPDATE therapists SET full_name = 'Dr. Bich Thai'    WHERE therapist_id = 'd95cd44e-543e-7d23-2a7f-76c1cf5eea39'::uuid;
UPDATE therapists SET full_name = 'Dr. Son Doan'     WHERE therapist_id = '138bf1e0-a096-a522-fae7-eae964290a81'::uuid;
UPDATE therapists SET full_name = 'Dr. Quynh Ta'     WHERE therapist_id = 'cec1c074-133c-1a56-1ffa-8e67f564b43f'::uuid;
UPDATE therapists SET full_name = 'Dr. Dat Lai'      WHERE therapist_id = '59fa09cb-22fb-f2c5-2903-1ae561badcaf'::uuid;
UPDATE therapists SET full_name = 'Dr. Diem Tang'    WHERE therapist_id = '327d3084-0b7f-420f-1a4b-f158dd4de2c0'::uuid;
UPDATE therapists SET full_name = 'Dr. Khoi Vuong'   WHERE therapist_id = 'ede12bcd-9193-b864-a71b-9f3c59c91d80'::uuid;
