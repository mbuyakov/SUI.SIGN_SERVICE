FROM nexus.suilib.ru/openjdk:8-alpine as java-jcp

ARG JCP_VERSION=jcp-2.0.41618

COPY ./buildfiles/${JCP_VERSION}.zip /opt/jcp.zip

RUN cd /opt && unzip -q jcp.zip && rm -f jcp.zip && \
    cd /opt/${JCP_VERSION}/ &&\
    cp -R ./dependencies/. ${JAVA_HOME}/jre/lib/ext/ &&\
    sh setup_console.sh ${JAVA_HOME}/jre -force -en -install -jcp -cades -jre ${JAVA_HOME}/jre &&\
    java ru.CryptoPro.JCP.Util.SetPrefs -system -node ru/CryptoPro/JCP/Random -key "Used BIORandom" -value "ru.CryptoPro.JCP.Random.BioRandomConsole"

FROM java-jcp as builder

RUN apk update && apk add maven

ADD pom.xml /app/pom.xml
RUN cd /app && mvn dependency:resolve

ADD src /app/src
RUN cd /app && mvn package

FROM java-jcp

COPY ./buildfiles/docker-entrypoint.sh /opt/
RUN chmod +x /opt/docker-entrypoint.sh
ENTRYPOINT ["/opt/docker-entrypoint.sh"]

COPY --from=builder /app/target/*.jar /opt/app.jar

RUN adduser signservice --system &&\
    chown signservice -R ${JAVA_HOME} &&\
    chown signservice -R /opt &&\
    chown signservice -R /var/opt/cprocsp/keys &&\
    chmod -R 777 ${JAVA_HOME}/jre/lib/security/cacerts

USER signservice