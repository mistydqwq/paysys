# Stock

## 项目介绍

这是一个基于 Spring Boot 的库存管理系统，提供库存查询、预留、释放等核心功能。主要用于订单流转聚合支付系统中的库存管理模块。

## 技术栈

- Java 17
- Spring Boot 3.3.1
- MySQL 8.0+
- Redis 6.2.5
- MyBatis Plus 3.5.7
- Swagger/Knife4j 4.5.0
- Dubbo 3.3.1
- Nacos 2.2.3

## API 路由表

### HTTP API
```shell
GET /api/v1/stocks/{productId} # 获取商品总库存
GET /api/v1/stocks/{productId}/reserve # 获取商品预留库存
GET /api/v1/stocks/{productId}/available # 获取商品可用库存
POST /api/v1/stocks/update # 更新商品库存
DELETE /api/v1/stocks/{productId} # 删除商品库存
```

### RPC API
```shell
rpc reserveStock(orderId, List<OrderItem>) # 预留商品库存
rpc releaseStock(orderId, List<OrderItem>) # 释放商品库存
```

## 本地开发指南

1. 启动 MySQL 服务，创建数据库 `stock`，建表 SQL 位于 `sql/create.sql`
2. 启动 Redis 服务 
3. 启动 Nacos 服务，创建数据库 `nacos_config`，建表 SQL 位于 `sql/createNacos.sql`，Docker 启动命令如下：
```shell
docker run --name nacos \
  -e MODE=standalone \
  -e JVM_XMS=512m \
  -e JVM_XMX=512m \
  -e SPRING_DATASOURCE_PLATFORM=mysql \
  -e MYSQL_SERVICE_HOST=host.docker.internal \
  -e MYSQL_SERVICE_PORT=3306 \
  -e MYSQL_SERVICE_DB_NAME=nacos_config \
  -e MYSQL_SERVICE_USER=root \
  -e MYSQL_SERVICE_PASSWORD=tyn192659 \
  -p 8848:8848 \
  -p 9848:9848 \
  -d nacos/nacos-server:v2.2.3
```
4. 启动 ServiceStockApplication

## Docker 部署

### 构建镜像并运行容器

1. 构建 Docker 镜像
```shell
docker build -t service-stock:latest .
```
2. 运行 Docker 容器
```shell
docker run -d \
  --name service-stock \
  -p 8080:8080 \
  -p 20880:20880 \
  -e MYSQL_HOST=host.docker.internal \
  -e REDIS_HOST=host.docker.internal \
  -e NACOS_HOST=host.docker.internal \
  service-stock:latest
```

### 使用 Docker Compose 部署

1. 启动服务
```shell
docker-compose up -d
```
2. 停止服务
```shell
docker-compose down
```

## 项目架构

