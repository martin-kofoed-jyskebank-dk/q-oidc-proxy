#!/bin/bash

podman stop oidc-mock
podman run -d --rm -p 9090:80 --name oidc-mock -e CLIENTS_CONFIGURATION_PATH=/tmp/config/oidc-clients-localhost.json -e USERS_CONFIGURATION_PATH=/tmp/config/oidc-users-localhost.json -v .:/tmp/config:ro docker.io/soluto/oidc-server-mock:0.3.2
podman logs -f oidc-mock
