FROM eclipse-temurin:21-jre

ARG APP_UID=10001
ARG APP_GID=10001

WORKDIR /app

# Feste numerische IDs erlauben Kubernetes, runAsNonRoot zuverlässig zu prüfen.
RUN groupadd --gid "${APP_GID}" appgroup \
    && useradd --uid "${APP_UID}" --gid "${APP_GID}" \
       --no-create-home --shell /usr/sbin/nologin appuser

COPY --chown=${APP_UID}:${APP_GID} build/libs/app.jar /app/app.jar

USER ${APP_UID}:${APP_GID}
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]
