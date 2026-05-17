package br.com.bancotoyota.services.simulador.config;

import jakarta.servlet.Filter;
import org.apache.catalina.connector.Connector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {
	@Value("${info.app.version}")
	private String versao;
    @Bean
    public TomcatServletWebServerFactory servletContainer(@Value(value = "${server.port}") Integer port) {
        TomcatServletWebServerFactory tomcatFactory = new TomcatServletWebServerFactory();
        Sl4j2AccessLog accessLog = new Sl4j2AccessLog();
        accessLog.setPattern("%h %l %u \"%r\" %s %b"); // o mesmo que o COMMON_ALIAS só que sem o timestamp que já é colocado pelo Log4j2
        accessLog.setCondition("health");
        tomcatFactory.addContextValves(accessLog);

        // estamos limitando o número de threads para um valor muito baixo, por isso precisamos usar um connector
        // diferente para o probe que caso contrário vai concorrer com os requests de negócio
        if (port != 0) { // se a porta for zero então se trata de unit test e não podemos usar essa porta na build
            Connector probeConnector = new Connector();
            probeConnector.setPort(8081); // REQ0069117
            tomcatFactory.addAdditionalTomcatConnectors(probeConnector);
        }

        Logger logger = LogManager.getLogger("access-log");
        logger.error(String.format("Versão da Api: %s",this.versao));
        logger.log(Level.ERROR, logger.getName() + " log level: " + logger.getLevel());
       
        return tomcatFactory;
    }

    @Autowired
    @Bean
    public FilterRegistrationBean<Filter> healthFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter((request, response, chain) -> {
            request.setAttribute("health", Boolean.TRUE);
            chain.doFilter(request, response);
        });
        registrationBean.addUrlPatterns("/actuator/*");

        return registrationBean;
    }
}
