FROM maven:3.6.1-jdk-11-slim as builder

# sett riktig tidssone
ENV TZ Europe/Oslo
RUN ln -fs /usr/share/zoneinfo/Europe/Oslo /etc/localtime

ADD / /source
WORKDIR /source
RUN mvn package -DskipTests

FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
COPY --from=builder /source/target/modiaeventdistribution-jar-with-dependencies.jar app.jar
