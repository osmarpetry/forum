# Forum

API REST de forum em Spring Boot 2.2 (Java 8). Primeiro contato com Spring Boot; depois sera reescrito em Kotlin.
O painel de monitoramento fica no repo [forum-springbootadmin](https://github.com/osmarpetry/forum-springbootadmin).

## Rodar com Docker (forum + admin)

Requisito: Docker. Os 2 repos clonados lado a lado:

```bash
git clone https://github.com/osmarpetry/forum.git
git clone https://github.com/osmarpetry/forum-springbootadmin.git
cd forum
docker compose up --build
```

Pronto quando o log mostrar `Started ForumApplication` (1a vez: 3-5 minutos, baixa as dependencias).

| O que | URL |
|---|---|
| API | http://localhost:8080/topics |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:forum`, user `sa`, sem senha) |
| Spring Boot Admin | http://localhost:8081 |

Parar: `docker compose down`.

## Rodar sem Docker

Requisito: Java 8+.

```bash
./mvnw spring-boot:run
```

Para ver no admin, suba tambem o `forum-springbootadmin` (porta 8081).

## Criterios de aceite (AC)

1. `GET /topics` responde 200 sem login e lista 2 topicos.
2. `POST /auth` com `aluno@email.com` / `123456` responde 200 com um `token`.
3. `POST`, `PUT`, `DELETE` em `/topics` sem token respondem 403.
4. `/swagger-ui.html` responde 200.
5. `/actuator/health` responde `{"status":"UP", ...}`.
6. O forum aparece no Spring Boot Admin (http://localhost:8081) com status UP.

## Como testar

Automatico (cobre AC 1, 2, 4 e o registro no admin do AC 6):

```bash
./mvnw test
```

Manual, com o app rodando:

```bash
curl -s localhost:8080/topics
```

```bash
curl -s -X POST localhost:8080/auth -H 'Content-Type: application/json' -d '{"email":"aluno@email.com","password":"123456"}'
```

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/topics -H 'Content-Type: application/json' -d '{}'
```

Esperado no ultimo comando: `403`.

Com token (troque `TOKEN` pelo valor do `/auth`):

```bash
curl -s -X POST localhost:8080/topics -H 'Authorization: Bearer TOKEN' -H 'Content-Type: application/json' -d '{"title":"Teste","message":"Mensagem de teste","courseName":"Spring Boot"}'
```

Postman: https://www.getpostman.com/collections/34173361d597c7558426

## Dependabot

- Roda toda semana (`.github/dependabot.yml`).
- So abre PR de **patch**. Minor e major sao ignorados: Spring Boot 2.2, spring-boot-admin-client 2.2 e jjwt 0.9 precisam subir juntos, e isso e manual.
- PR do dependabot com CI verde faz merge sozinho (`.github/workflows/ci.yml`).

## Seguranca (conhecido)

`src/main/java/com/forum/forum/security/SecurityConfigurations.java` libera o `/actuator/**` para todos. Isso e errado em producao. O Spring Boot Admin tambem e publico.
