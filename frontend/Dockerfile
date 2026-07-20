# --- Build stage ---------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
RUN npm run build

# --- Serve stage ---------------------------------------------------------
FROM nginx:1.27-alpine

# The nginx image runs envsubst on templates in /etc/nginx/templates at startup,
# so BACKEND_URL is injected into the config without rebuilding the image.
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

ENV BACKEND_URL=http://backend:8080
EXPOSE 80
