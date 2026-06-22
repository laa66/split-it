# Prod web tier: builds the frontend bundle, serves it via nginx, proxies /api to backend.
# Build context is the repo root (see docker-compose.yml `web` service).

FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine AS prod
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
