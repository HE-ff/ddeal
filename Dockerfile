FROM openjdk:11-slim as build
COPY . /home/ddeal
WORKDIR /home/ddeal
RUN ./gradlew build


FROM openjdk:11-slim as run
RUN addgroup --system ddeal && adduser --system ddeal --ingroup ddeal
USER ddeal:ddeal
COPY --from=build /home/ddeal/build/libs/*.jar /home/ddeal/ddeal.jar
WORKDIR /home/ddeal
ENTRYPOINT ["java","-jar","ddeal.jar"]
