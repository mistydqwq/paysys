# 使用 JDK 17 作为基础镜像
FROM eclipse-temurin:17-jdk-focal as builder

# 设置工作目录
WORKDIR /app

# 复制 Maven 配置和源代码
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src/

# 构建应用
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# 使用 JRE 作为运行时镜像
FROM eclipse-temurin:17-jre-focal

# 工作目录
WORKDIR /app

# 复制构建产物
COPY --from=builder /app/target/service-stock-*.jar app.jar

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai

# 暴露端口
EXPOSE 8080
EXPOSE 20880

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 设置容器启动命令
ENTRYPOINT ["java", \
    "-Xms512m", \
    "-Xmx512m", \
    "-jar", \
    "app.jar", \
    "--spring.datasource.url=jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:payment}", \
    "--spring.datasource.username=${MYSQL_USER:root}", \
    "--spring.datasource.password=${MYSQL_PASSWORD:tyn192659}", \
    "--spring.redis.host=${REDIS_HOST:localhost}", \
    "--spring.redis.port=${REDIS_PORT:6379}", \
    "--spring.redis.password=${REDIS_PASSWORD:tyn192659}", \
    "--spring.cloud.nacos.discovery.server-addr=${NACOS_HOST:localhost}:${NACOS_PORT:8848}"]