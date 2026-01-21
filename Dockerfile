# 1. Aşama: Uygulamayı derle
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Sadece pom.xml'i kopyalayıp bağımlılıkları indir (cache avantajı için)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala ve uygulamayı paketle
COPY src ./src
RUN mvn package -DskipTests

# 2. Aşama: Çalışma ortamı
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Derlenen JAR dosyasını kopyala
COPY --from=build /app/target/aws-transcribe-sample-application-1.0-SNAPSHOT.jar app.jar

# Uygulama portunu belirt
EXPOSE 8080

# Uygulamayı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
