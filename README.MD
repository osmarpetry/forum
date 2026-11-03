## Forum 
The intention of this project I have the first contact with Spring Boot using Java, then re-write to Kotlin.

## How to start the project
Needs Java 17+. Start the admin first ([forum-springbootadmin](https://github.com/osmarpetry/forum-springbootadmin)), then:

```mvn spring-boot:run```

Login: `POST /auth` with `{"email":"aluno@email.com","password":"123456"}`.

## How to run with Docker (local or Synology)
Runs the forum (8080), the admin (8081, built from the other repo) and, optionally, a Cloudflare Tunnel.

```
cp .env.example .env   # fill FORUM_JWT_SECRET (and CLOUDFLARE_TUNNEL_TOKEN for the tunnel)
docker compose up -d --build
docker compose --profile tunnel up -d --build   # with Cloudflare Tunnel
```

Synology: Container Manager > Project > Create > point to this folder (with `.env`).
Cloudflare: Zero Trust > Networks > Tunnels > create a tunnel, copy its token to `.env`,
and add a public hostname to `http://forum:8080`. Keep the admin (`http://admin:8081`) private
or behind Cloudflare Access - it has no login.

## How to test with Postman
Just try this link:

```https://www.getpostman.com/collections/34173361d597c7558426```

## Web UI
A small server-rendered UI (Thymeleaf + htmx 2.0.11, no npm, no CDN) runs next to the JSON API:

```http://localhost:8080/```

- Everyone can list topics (filter by course name, pages) and read a topic with its answers.
- Log in at `/ui/login` (seed user `aluno@email.com`) to create, edit and delete topics. The UI uses a session cookie and CSRF; the JSON API still uses the JWT token.
- A UI write clears the `topicsList` cache, so `GET /topics` shows the change at once.
- htmx lives in `src/main/resources/static/ui/htmx.min.js`. Templates are in `src/main/resources/templates`.

## How to access the Swagger
Just try this link, after started the project:

```http://localhost:8080/swagger-ui.html```
 
 
### About security with Spring Boot actuator and Spring Boot admin
In Docker (profile `prod`) the actuator runs on port 8082. Compose does not publish this port, so only the admin container can reach it. Port 8080 does not serve `/actuator` or `/h2-console`. Without Docker (default profile) the actuator is on 8080 and open to everyone: use that only on your machine.

## Run with Docker (daily use, Mac and Synology)
The forum and the admin run from 1 `docker-compose.yml`. The admin is built from the forum-springbootadmin repo at a pinned commit.

```bash
cp .env.example .env   # then set FORUM_JWT_SECRET=$(openssl rand -base64 48)
./deploy.sh            # git pull, build, start, check: /topics, /auth, Swagger, admin shows forum UP
./deploy.sh --tunnel   # the same, plus the Cloudflare Tunnel (set CLOUDFLARE_TUNNEL_TOKEN in .env)
```

- Forum: http://localhost:8080. Admin: http://localhost:8081.
- Stop: `docker compose down`. Logs: `docker compose logs -f forum`.
- Data: the H2 database files are in `./data`. Back up this folder. The container runs as root, so on Synology the files belong to root: a normal DSM user cannot delete them.

## Build without Docker
Java comes from asdf (`.tool-versions`): `asdf plugin add java && asdf install`.

```bash
./mvnw package           # build and test
./mvnw spring-boot:run   # default profile: in-memory database, data is lost at stop
```
