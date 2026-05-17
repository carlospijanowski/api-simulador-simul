FROM eclipse-temurin:21 AS builder
WORKDIR /tmp/application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted
FROM eclipse-temurin:21
WORKDIR /tmp/application
EXPOSE 7774


ENV NEWFRONT_REDIS_PASSWORD changeme
ENV JAVA_DEBUG ""
ENV MONGODB_URI mongodb://mongoadm:changeme@mongodb-0.mongodb-internal.direct.svc.cluster.local:27017,mongodb-1.mongodb-internal.direct.svc.cluster.local:27017,mongodb-2.mongodb-internal.direct.svc.cluster.local:27017/admin

ENV JAVA_OPTS " "
ENV API_CLIENTES " "
ENV API_EMPLACAMENTOS " "
ENV API_ORIGENS_NEGOCIOS " "
ENV API_CESTAS_SERVICOS " "
ENV API_SEGURO_MECANICA_URL " "
ENV API_PLANOS " "
ENV API_MOTOR_TAXAS " "
ENV MAX_THREADS 8
ENV MAX_REQUESTS_EXTERNOS 0
ENV ACTIVE_PROFILE dev
ENV REDIS_HOSTNAME redis
ENV SERVER_PORT 7774
ENV SPRING_OPTS ""
ENV API_VEICULOS " "
ENV API_FINANCIAMENTOS_PROPOSTAS " "
ENV HABILITAR_VEICULOS false
ENV HABILITAR_SIMULACAO_PARCEIROS false
ENV NIVEL_LOG_GERAL DEBUG
ENV NIVEL_LOG_ESPECIFICO DEBUG
ENV MONGODB_PARCEIROS_URI " "
ENV HABILITAR_FILTRO_ID_MODALIDADE false
ENV LIMITE_RETENTATIVAS 5
ENV DELAY_ENTRE_RETENTATIVAS 1000

ENV SERVICE_URL " "
ENV TOKEN_URL " "
ENV CLIENT_ID_PARCEIROS " "
ENV CLIENT_SECRET_PARCEIROS " "
ENV USERNAME_PARCEIROS " "
ENV PASSWORD_PARCEIROS " "
ENV API_PRESTAMISTA_PLUS_URL " "
ENV HABILITAR_CONSULTA_SEGURO_PRESTAMISTA true
ENV DIRECT_USERNAME ""
ENV DIRECT_PASSWORD ""
ENV DIRECT_CLIENT_ID ""
ENV DIRECT_CLIENT_SECRET ""
ENV MOTOR_TAXAS_ENABLED false
ENV API_MOTOR_TAXAS_URL " "

COPY --from=builder /tmp/application/extracted/dependencies/ ./
COPY --from=builder /tmp/application/extracted/snapshot-dependencies/ ./
COPY --from=builder /tmp/application/extracted/spring-boot-loader/ ./
COPY --from=builder /tmp/application/extracted/application/ ./


# -agentlib:jdwp=transport=dt_socket,address=5000,suspend=y,server=y
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher", \
    "--api.clientes.url=${API_CLIENTES}", \
    "--api.emplacamentos.url=${API_EMPLACAMENTOS}", \
    "--api.origens-negocios.url=${API_ORIGENS_NEGOCIOS}", \
    "--api.cestas-servicos.url=${API_CESTAS_SERVICOS}", \
    "--api.planos.url=${API_PLANOS}", \
    "--api.propostas-financiamento.url=${API_FINANCIAMENTOS_PROPOSTAS}", \
    "--api.seguro.garantia.url=${API_SEGURO_MECANICA_URL}", \
    "--api.veiculos.url=${API_VEICULOS}", \
    "--api.veiculos.enabled=${HABILITAR_VEICULOS}", \
    "--planos.filtro-id-modalidade.enabled=${HABILITAR_FILTRO_ID_MODALIDADE}", \
    "--simulacao-simplificada-parceiros.enabled=${HABILITAR_SIMULACAO_PARCEIROS}", \
    "--spring.data.mongodb.uri=${MONGODB_URI}", \
    "--mongodb.propostas.uri=${MONGODB_URI}", \
    "--mongodb.simulacoes-parceiros.uri=${MONGODB_PARCEIROS_URI}", \
    "--server.tomcat.max-threads=${MAX_THREADS}", \
    "--simulador.max-requests-externos=${MAX_REQUESTS_EXTERNOS}", \
    "--spring.profiles.active=${ACTIVE_PROFILE}", \
    "--spring.redis.host=${REDIS_HOSTNAME}", \
    "--spring.redis.port=${REDIS_PORT}", \
    "--server.port=${SERVER_PORT}", \
    "--logging.level.root=${NIVEL_LOG_GERAL}",  \
    "--logging.level.access-log=${NIVEL_LOG_ESPECIFICO}", \
    "--logging.level.org.springframework=${NIVEL_LOG_ESPECIFICO}", \
    "--logging.level.br.com.bancotoyota.services.simulador.controller=${NIVEL_LOG_ESPECIFICO}", \
    "--logging.level.br.com.bancotoyota.services.simulador.services.LogService=${NIVEL_LOG_ESPECIFICO}",\
    "--logging.level.org.apache.tomcat.util.threads.LimitLatch=${NIVEL_LOG_ESPECIFICO}",\
    "--logging.level.br.com.bancotoyota.services.simulador.services.IntermediariasService=${NIVEL_LOG_ESPECIFICO}",\
    "--logging.level.br.com.bancotoyota.services.simulador.utils.PrazoMaisProximoValorDesejadoUtils=${NIVEL_LOG_ESPECIFICO}",\
    "--spring.retry.numero-retentativas=${LIMITE_RETENTATIVAS}",\
    "--spring.retry.delay-entre-retentativas-ms=${DELAY_ENTRE_RETENTATIVAS}",\
	"--security.oauth2.service-url=${SERVICE_URL}",\
	"--security.oauth2.token-url=${TOKEN_URL}",\
	"--security.oauth2.client-id-parceiros=${CLIENT_ID_PARCEIROS}",\
	"--security.oauth2.client-secret-parceiros=${CLIENT_SECRET_PARCEIROS}",\
	"--security.oauth2.username-parceiros=${USERNAME_PARCEIROS}",\
	"--security.oauth2.password-parceiros=${PASSWORD_PARCEIROS}",\
	"--seguro.prestamista.plus.url=${API_PRESTAMISTA_PLUS_URL}",\
	"--simulador.consulta.seguro.prestamista.enabled=${HABILITAR_CONSULTA_SEGURO_PRESTAMISTA}",\
	"--security.oauth2.direct-username=${DIRECT_USERNAME}",\
    "--security.oauth2.direct-password=${DIRECT_PASSWORD}",\
    "--security.oauth2.direct-client-id=${DIRECT_CLIENT_ID}",\
    "--security.oauth2.direct-client-secret=${DIRECT_CLIENT_SECRET}",\
    "--busca-api-motor-taxa.enabled=${MOTOR_TAXAS_ENABLED}",\
    "--api.motor-taxa.url=${API_MOTOR_TAXAS_URL}",\
    "${SPRING_OPTS}" ]
