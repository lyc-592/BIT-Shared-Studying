#!/bin/bash
set -e

echo "🚀 开始部署Sharing应用到Docker容器..."

# 1. 停止当前运行的jar进程
echo "停止当前运行的应用进程..."
pkill -f "java.*Sharing" 2>/dev/null || echo "没有找到运行的Sharing进程"

# 2. 停止并删除已有的Docker容器
echo "清理已有的Docker容器..."
docker stop sharing-app 2>/dev/null || echo "没有运行的sharing-app容器"
docker rm sharing-app 2>/dev/null || echo "没有sharing-app容器可删除"

# 3. 构建项目（使用Maven）
echo "使用Maven构建项目..."
mvn clean package -DskipTests

# 4. 构建Docker镜像
echo "构建Docker镜像..."
docker build -t sharing-app .

# 5. 运行Docker容器（关键步骤：保持原样）
echo "启动Docker容器..."
docker run -d \
  --name sharing-app \
  --network host \
  -v /root/mysql_file:/root/mysql_file \
  -v /root/sharing_files:/root/sharing_files \
  -v /root/Apply:/root/Apply \
  -v /root/unsure:/root/unsure \
  -p 8080:8080 \
  --restart unless-stopped \
  sharing-app

# 6. 等待应用启动
echo "等待应用启动（15秒）..."
sleep 15

# 7. 验证部署结果
echo "验证部署结果："
echo "1. 容器状态："
docker ps | grep sharing-app

echo ""
echo "2. 应用日志（最后10行）："
docker logs --tail 10 sharing-app

echo ""
echo "3. 检查端口监听："
netstat -tlnp | grep :8080

echo ""
echo "✅ 部署完成！"
echo "应用访问地址：http://47.94.122.20:8080"
echo ""
echo "📋 管理命令："
echo "   查看日志：docker logs -f sharing-app"
echo "   重启应用：docker restart sharing-app"
echo "   停止应用：docker stop sharing-app"
echo "   进入容器：docker exec -it sharing-app /bin/bash"
