FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:10.1-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/NetBanking.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/netbanking-entrypoint
RUN chmod +x /usr/local/bin/netbanking-entrypoint

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/netbanking-entrypoint"]
