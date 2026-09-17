# Multi-stage build: compile with the Maven Wrapper against a pinned JDK 21,
# then run on a minimal JRE 21 image. No build tools ship in the final image.

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data/storage && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/8080 && echo -e "GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
