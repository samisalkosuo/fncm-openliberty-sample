# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY lib ./lib
COPY pom.xml .

# Install the local FileNet JARs into the Maven local repository so that
# mvn package picks them up reliably (system-scope JARs are excluded from
# WAR packaging by Maven WAR plugin 3.x unless installed this way).
RUN mvn install:install-file -Dfile=lib/Jace.jar \
        -DgroupId=com.ibm.filenet -DartifactId=Jace \
        -Dversion=5.5 -Dpackaging=jar -q \
 && mvn install:install-file -Dfile=lib/p8cel10n.jar \
        -DgroupId=com.ibm.filenet -DartifactId=p8cel10n \
        -Dversion=5.5 -Dpackaging=jar -q

#download from Maven before adding our source
RUN mvn package

COPY src ./src
RUN mvn package -DskipTests

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM icr.io/appcafe/open-liberty:full-java21-openj9-ubi-minimal AS runtime

# Install only the features declared in server.xml
COPY --from=build /build/src/main/liberty/config/server.xml \
     /config/server.xml

RUN features.sh

# Copy the application WAR
COPY --from=build /build/target/app.war /config/apps/app.war

EXPOSE 9080 9443

# Run as non-root (Liberty default user is 1001)
USER 1001

CMD ["/opt/ol/helpers/runtime/docker-server.sh", "/opt/ol/wlp/bin/server", "run", "defaultServer"]
