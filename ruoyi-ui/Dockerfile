FROM node:22-alpine AS build
WORKDIR /app
COPY ruoyi-ui/package*.json ./
RUN npm install
COPY ruoyi-ui/ ./
RUN npm run build:prod

FROM nginx:1.27-alpine
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
