#!/bin/bash

podman stop oidc-mock
podman run -d --rm -p 9090:8080 --name oidc-mock \
    -e CLIENTS_CONFIGURATION_PATH=/tmp/config/oidc-clients-localhost.json \
    -e SERVER_OPTIONS_PATH=/tmp/config/oidc-server-options.json \
    -e USERS_CONFIGURATION_PATH=/tmp/config/oidc-users-localhost.json \
    -v .:/tmp/config:ro ghcr.io/soluto/oidc-server-mock:latest
podman logs -f oidc-mock
