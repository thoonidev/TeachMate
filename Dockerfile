# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY database/schema.sql ./database/schema.sql
RUN mvn --batch-mode --no-transfer-progress clean package

FROM tomcat:9.0.123-jdk17-temurin-noble
ENV PORT=10000 \
    SESSION_COOKIE_SECURE=true \
    TRUST_RENDER_PROXY=false \
    CATALINA_OPTS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8"

COPY deploy/server.xml /usr/local/tomcat/conf/server.xml
COPY deploy/start.sh /usr/local/bin/start-teachmate
COPY --from=build /build/target/TeachMate.war /tmp/teachmate.war
RUN groupadd --gid 10001 teachmate \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin teachmate \
    && rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist \
    && mkdir -p /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/conf/Catalina/localhost \
    && cd /usr/local/tomcat/webapps/ROOT \
    && jar -xf /tmp/teachmate.war \
    && rm /tmp/teachmate.war \
    && chmod 755 /usr/local/bin/start-teachmate \
    && chown -R teachmate:teachmate /usr/local/tomcat/logs /usr/local/tomcat/temp /usr/local/tomcat/work

USER 10001:10001
EXPOSE 10000
ENTRYPOINT ["/usr/local/bin/start-teachmate"]
