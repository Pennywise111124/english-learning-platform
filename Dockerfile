# ---- Giai đoạn build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Xóa CRLF (nếu mvnw được checkout trên Windows) và cấp quyền chạy
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# Lớp này được cache khi pom.xml không đổi
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Bỏ cả biên dịch test: giai đoạn build không có Docker cho Testcontainers
RUN ./mvnw -B -q -Dmaven.test.skip=true package

# ---- Giai đoạn chạy ----
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --uid 10001 app
WORKDIR /app
RUN mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /workspace/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]