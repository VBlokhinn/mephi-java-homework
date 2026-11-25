FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY . .

RUN chmod +x gradlew && ./gradlew :personal-finance:installDist --no-daemon --info

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /build/personal-finance/build/install/personal-finance/ ./

USER appuser

ENTRYPOINT ["bin/personal-finance"]
