# BUILD IMAGE
FROM adoptopenjdk:11-jdk as BUILD
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"
WORKDIR /builder

# Cache gradle
COPY gradle ./gradle
COPY gradlew ./
RUN ./gradlew --version

# Cache dependencies
COPY build.gradle settings.gradle ./
RUN ./gradlew downloadDependencies

# Build
#COPY config ./config
COPY src ./src
RUN ./gradlew --offline installDist

# RUNTIME IMAGE
FROM adoptopenjdk:11-jre
COPY --from=BUILD /builder/build/install/s3-glob-sync /opt/s3-glob-sync
COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]
CMD ["-h"]
