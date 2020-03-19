## Forum 
The intention of this project I have the first contact with Spring Boot using Java, then re-write to Kotlin.

## How to start the project
To start the project, run the following command:

```mvn spring-boot:run```

## How to test with Postman
Just try this link:

```https://www.getpostman.com/collections/34173361d597c7558426```

## How to access the Swagger
Just try this link, after started the project:

```http://localhost:8080/swagger-ui.html#/topics-controller```
 
 
 ### About security with Spring Boot actuator and Spring Boot admin 
 In the file ```src/main/java/com/forum/forum/security/SecurityConfigurations.java``` I'm allowing everyone to access actuator, this is wrong. And the Spring boot admin (in the other repo.) is public, like the actuator configuration here.