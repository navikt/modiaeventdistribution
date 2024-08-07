#!/usr/bin/env bash

# Enables remote debugging on port 5005 if the application is running in one of the test clusters

if [ "$NAIS_CLUSTER_NAME" = "dev-gcp" ] || [ "$NAIS_CLUSTER_NAME" = "dev-sbs" ]; then
  export JAVA_OPTS="${JAVA_OPTS} -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n"
fi
