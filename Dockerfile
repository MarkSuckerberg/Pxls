
# Force Build 8
FROM maven:3.9.11-eclipse-temurin-17-alpine AS deps


# Download dependencies as a separate step to take advantage of Docker's caching.
# Leverage a cache mount to /root/.m2 so that subsequent builds don't have to
# re-download packages.
RUN --mount=type=bind,source=pom.xml,target=pom.xml 

FROM deps AS build
COPY . /Pxls/
WORKDIR /Pxls
RUN java -version
RUN mvn clean package; \
    mkdir /tmp/pxls; \
    cp target/pxls*.jar /tmp/pxls/pxls.jar; \
    cp -r resources/* /tmp/pxls

FROM eclipse-temurin:17-jdk-alpine

RUN apk add\ 
    curl

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

LABEL maintainer="Aneurin Price adp@nyeprice.space"
COPY --from=build /tmp/pxls /tmp/
COPY entrypoint.d/ /entrypoint.d
HEALTHCHECK CMD curl --fail http://localhost:4567/users || exit 1
ENTRYPOINT [ "/bin/run-parts", "--exit-on-error", "/entrypoint.d" ]
