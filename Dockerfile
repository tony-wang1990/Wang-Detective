FROM maven:3-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests \
    && cp target/king-detective-*.jar /app/king-detective.jar

FROM eclipse-temurin:21-jre-jammy AS base-with-tools

ENV LANG=zh_CN.UTF-8 \
    LC_ALL=zh_CN.UTF-8 \
    TZ=Asia/Shanghai

RUN apt update && \
    apt install -y --no-install-recommends openssh-client lsof curl locales && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /root/.ssh && \
    echo "Host *\n  HostKeyAlgorithms +ssh-rsa\n  PubkeyAcceptedKeyTypes +ssh-rsa" > /root/.ssh/config && \
    chmod 700 /root/.ssh && chmod 600 /root/.ssh/config && \
    locale-gen zh_CN.UTF-8 && \
    ln -fs /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

FROM base-with-tools

ENV KING_DETECTIVE_VERSION=3.0.1

WORKDIR /app/king-detective

COPY --from=builder /app/king-detective.jar .

# Declare volumes for persistent data
VOLUME ["/app/king-detective/data", "/app/king-detective/keys"]

EXPOSE 9527

CMD exec java \
    --add-opens java.base/java.net=ALL-UNNAMED \
    --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED \
    -jar king-detective.jar | tee -a /var/log/king-detective.log
