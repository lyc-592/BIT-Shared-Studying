FROM openjdk:21-slim

# 设置时区为上海
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建应用运行所需的工作目录（保持与现有完全一致）
RUN mkdir -p /root/mysql_file /root/sharing_files /root/Apply /root/unsure

# 将打包好的jar包复制到容器中
COPY target/Sharing-0.0.1-SNAPSHOT.jar /app.jar

# 暴露应用端口
EXPOSE 8080

# 启动命令 - 使用你原有的application.yml配置
ENTRYPOINT ["java", "-jar", "/app.jar"]