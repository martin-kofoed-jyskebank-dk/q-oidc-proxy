#!/bin/bash

# Environment vars for localhost:
export AUTHENTICATION_TYPE=COOKIE
export AUTHENTICATION_COOKIE_NAME=q_auth_proxy
export AUTHENTICATION_COOKIE_DOMAIN=localhost
export AUTHENTICATION_COOKIE_SAMESITE=LAX
export AUTHENTICATION_FRONTEND_REDIRECT=https://echo.free.beeceptor.com
export AUTHENTICATION_FRONTEND_CALLBACK_PARAM=authcode
export AUTHENTICATION_BACKEND_HEADER_NAME=Authorization
export AUTHENTICATION_ALLOW_ORIGINS="/https://([a-z0-9\\-_]+)localhost/"
export OIDC_PROVIDER_BASE_URL=http://localhost:9090
export OIDC_CLIENT_ID=dummy
export OIDC_CLIENT_SECRET=dummy
export OIDC_REDIRECT_URI=http://localhost:8080/oidc/callback

export BACKEND_HOST_BASE_URL=https://echo.free.beeceptor.com
export BACKEND_HEADER_PROPAGATION=Authorization,X-Correlation-Id

# Run quarkus in dev mode
quarkus dev 
