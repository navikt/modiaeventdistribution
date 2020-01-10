FROM maven:3.6.1-jdk-8-alpine as builder

# sett riktig tidssone
ENV TZ Europe/Oslo
RUN ln -fs /usr/share/zoneinfo/Europe/Oslo /etc/localtime

ADD / /source
WORKDIR /source
RUN mvn package -DskipTests

FROM navikt/java:8-appdynamics
ENV APPD_ENABLED=true
COPY --from=builder /source/target/modiaeventdistribution /app
