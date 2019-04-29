FROM adoptopenjdk/maven-openjdk9
COPY . ./leafactor-ci
WORKDIR ./leafactor-ci

RUN mvn install "-DskipTests"
