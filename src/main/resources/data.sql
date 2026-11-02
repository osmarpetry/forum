/* Seed data. Each insert runs only when its row is missing, so a file database can start many times. */

/* Password is 123456 */
INSERT INTO USER(name, email, password)
SELECT 'Aluno', 'aluno@email.com', '$2a$10$SpFzIQU5Uhg2X0Z08Eq2HuibkYmshIS3ZCikorZUwo.pE34Z8O1C.'
WHERE NOT EXISTS (SELECT 1 FROM USER WHERE email = 'aluno@email.com');

INSERT INTO COURSE(name, category)
SELECT 'Spring Boot', 'Programming'
WHERE NOT EXISTS (SELECT 1 FROM COURSE WHERE name = 'Spring Boot');

INSERT INTO TOPIC(title, message, creation_date, status, author_id, course_id)
SELECT 'Doubt', 'Error to create the project', '2019-05-05 18:00:00', 'NOT_ANSWERED',
       (SELECT id FROM USER WHERE email = 'aluno@email.com'), (SELECT id FROM COURSE WHERE name = 'Spring Boot')
WHERE NOT EXISTS (SELECT 1 FROM TOPIC WHERE message = 'Error to create the project');

INSERT INTO TOPIC(title, message, creation_date, status, author_id, course_id)
SELECT 'Doubt', 'Error to do error handler', '2020-05-05 18:00:00', 'NOT_ANSWERED',
       (SELECT id FROM USER WHERE email = 'aluno@email.com'), (SELECT id FROM COURSE WHERE name = 'Spring Boot')
WHERE NOT EXISTS (SELECT 1 FROM TOPIC WHERE message = 'Error to do error handler');
