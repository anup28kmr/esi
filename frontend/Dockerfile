# syntax=docker/dockerfile:1

# Build stage -- compile the Vue 3 app to static assets.
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first so the layer can cache across source edits.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .

# `VUE_APP_API_BASE_URL` is what Vue CLI bakes into the bundle. Default
# matches `.env.production` (http://localhost:9090 -- the api-gateway's
# host-published port). Override at build time to point the SPA at a
# different gateway.
ARG VUE_APP_API_BASE_URL
ENV VUE_APP_API_BASE_URL=${VUE_APP_API_BASE_URL}
RUN npm run build

# Runtime stage -- serve static assets only. The browser calls the
# api-gateway directly, so there is no /api reverse proxy here.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
