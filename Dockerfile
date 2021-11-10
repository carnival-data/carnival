FROM gradle:6-jdk11

### Development Image
ENV CARNIVAL_HOME /usr/carnival_home
#ENV APOC_HOME /usr/Neo4j/plugins
#ENV APOC_VERSION 3.4.0.7


ENV GRADLE_OPTS ${JAVA_OPTS}
ENV GRADLE_USER_HOME /home/gradle


ENV APP_SRC /opt/carnival
RUN mkdir -p ${APP_SRC}/carnival-core ${APP_SRC}/carnival-util ${APP_SRC}/carnival-graph

COPY app/*.gradle                           ${APP_SRC}
COPY app/gradle.properties                  ${APP_SRC}
COPY app/carnival-core/*.gradle             ${APP_SRC}/carnival-core
COPY app/carnival-util/*.gradle             ${APP_SRC}/carnival-util
COPY app/carnival-graph/*.gradle            ${APP_SRC}/carnival-graph

WORKDIR ${APP_SRC}
RUN gradle resolveRuntimeDependencies --no-daemon --console=plain


# Install linux utils
RUN apt-get update --fix-missing && apt-get install -y \
    dos2unix rename sed\
    --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# install APOC
#RUN mkdir -p ${APOC_HOME}
#ADD "https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/${APOC_VERSION}/apoc-${APOC_VERSION}-all.jar" $APOC_HOME


# set up CARNIVAL_HOME directory
WORKDIR ${CARNIVAL_HOME}/config

COPY config/logback.xml-travis-template logback.xml
COPY config/*yml-template /${CARNIVAL_HOME}/config/
RUN rename -v 's/-template//' *-template
RUN dos2unix **

RUN mkdir -p ${CARNIVAL_HOME}/target \
    ${CARNIVAL_HOME}/data/cache

# update APOC location in application.yml config file
# RUN sed -i "s#/path/to/neo4j/plugins#${APOC_HOME}#" application.yml
