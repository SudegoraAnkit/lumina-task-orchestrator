# Issue Summary

## Overview
This issue report summarizes the runtime failures encountered while starting the Spring Boot orchestrator application and the steps taken to resolve them.

## Errors Encountered

1. **PostgreSQL authentication failure**
   - `FATAL: password authentication failed for user "postgres"`
   - Hibernate was unable to obtain a JDBC connection metadata query because the database connection failed.
   - This was caused by the application connecting to `localhost:5432` on the host machine, where a local Windows PostgreSQL service was already listening.
   - The containerized PostgreSQL was mapped to the same port, causing a port collision and a mismatch in credentials.

2. **Hibernate dialect initialization failure**
   - `Unable to determine Dialect without JDBC metadata`
   - This was a downstream failure caused by the JDBC connection failure above.
   - Hibernate could not determine the PostgreSQL dialect because it could not connect to the database.

3. **Invalid PostgreSQL timezone setting**
   - `FATAL: invalid value for parameter "TimeZone": "Asia/Calcutta"`
   - The PostgreSQL server rejected the timezone parameter provided by the JVM or session settings.
   - This prevented the JDBC connection from initializing even after fixing the auth issue.

5. **Kafka producer Jackson class missing**
  - Runtime error when publishing a task: `java.lang.ClassNotFoundException: com.fasterxml.jackson.databind.JavaType`.
  - Root cause: Kafka's JSON serializer/deserializer expects Jackson databind on the classpath but it was not declared as a dependency.
  - Impact: Kafka producer failed to initialize causing request processing to throw an exception.

4. **Spring Data module configuration warning**
   - `Multiple Spring Data modules found, entering strict repository configuration mode`
   - This warning appeared because both Spring Data JPA and Spring Data MongoDB dependencies were present.
   - The application only declares a JPA repository, so MongoDB repository scanning did not find any MongoDB repositories.
   - This is informational and not the primary startup failure.

## Files Updated

- `src/main/resources/application.yml`
  - changed `spring.datasource.url` from `localhost:5432` to `localhost:5433`
  - added `driver-class-name: org.postgresql.Driver`
  - added `hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect`
  - added `hibernate.jdbc.time_zone: UTC`
  - added `com.fasterxml.jackson.core:jackson-databind` dependency to ensure `com.fasterxml.jackson.databind.JavaType` is available for Kafka JSON serialization

- `docker-compose.yml`
  - changed PostgreSQL port mapping from `5432:5432` to `5433:5432`

- `pom.xml`
  - added JVM timezone override in `spring-boot-maven-plugin`: `-Duser.timezone=UTC`

## Resolution Steps Followed

1. Reviewed the Spring Boot startup logs and identified the first failure as a PostgreSQL auth issue.
2. Confirmed the container environment variables and user credentials inside the `luminatask` container.
3. Checked the host machine and found a local `postgres.exe` process already listening on port `5432`.
4. Updated `docker-compose.yml` to publish container PostgreSQL on host port `5433` instead of `5432`.
5. Updated `src/main/resources/application.yml` to use `jdbc:postgresql://localhost:5433/luminatask`.
6. Added explicit PostgreSQL driver and Hibernate dialect configuration to the application properties.
7. Added timezone normalization settings to avoid the invalid timezone value.
8. Restarted the compose stack and verified `localhost:5433` was listening for Postgres.
9. Prepared the application to be restarted with the fixed configuration.
10. Added `jackson-databind` to `pom.xml` so Kafka's JSON serializer can construct required Jackson types.

## Recommended Next Steps

- Run the Spring Boot application again:
  - `./mvnw -DskipTests spring-boot:run`
- If you want to use host port `5432` for the container, stop or reconfigure the local Windows PostgreSQL service first.
- If the timezone issue persists, check any OS or JVM environment variables such as `TZ` or `user.timezone`.
 - If you see the Kafka ClassNotFoundException again, ensure `com.fasterxml.jackson.core:jackson-databind` is on the classpath and then rebuild.
