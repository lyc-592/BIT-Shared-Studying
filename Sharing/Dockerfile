# 使用官方Java基础镜像
FROM openjdk:26-ea-21-jdk-slim

# 设置工作目录
WORKDIR /app

# 拷贝构建好的jar包
ADD ./target/Sharing-1.0-SNAPSHOT.jar DemoApp.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["sh", "-c", "java -jar DemoApp.jar"]
