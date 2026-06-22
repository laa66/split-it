# --- deps ---
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json ./
RUN npm install

# --- dev: Vite dev server, source mounted via volume ---
FROM deps AS dev
WORKDIR /app
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# --- build static bundle (consumed by the nginx service in prod) ---
FROM deps AS build
WORKDIR /app
COPY . .
RUN npm run build
