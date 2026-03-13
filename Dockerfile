FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
	   ffmpeg \
	   libstdc++6 \
	   libglib2.0-0 \
	   libsm6 \
	   libxext6 \
	   libxrender1 \
	&& rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
