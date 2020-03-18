/* Password is 123456 */
INSERT INTO USER(name, email, password) VALUES('Aluno', 'aluno@email.com', '$2a$10$SpFzIQU5Uhg2X0Z08Eq2HuibkYmshIS3ZCikorZUwo.pE34Z8O1C.');

INSERT INTO COURSE(name, category) VALUES('Spring Boot', 'Programming');

INSERT INTO TOPIC(title, message, creation_date, status, author_id, course_id) VALUES('Doubt', 'Error to create the project', '2019-05-05 18:00:00', 'NOT_ANSWERED', 1, 1);
INSERT INTO TOPIC(title, message, creation_date, status, author_id, course_id) VALUES('Doubt', 'Error to do error handler', '2020-05-05 18:00:00', 'NOT_ANSWERED', 1, 1);