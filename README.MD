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

## How to access the Swagger
Just try this link, after started the project:

```http://localhost:8080/swagger-ui.html```
 
 
 ### About security with Spring Boot actuator and Spring Boot admin 
 In the file ```src/main/java/com/forum/forum/security/SecurityConfigurations.java``` I'm allowing everyone to access actuator, this is wrong. And the Spring boot admin (in the other repo.) is public, like the actuator configuration here.