package br.com.evertontrindade.nosql.configuration;

import org.h2.server.web.WebServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by everton on 27/05/16.
 */
@Configuration
public class WebConfiguration {

    @Bean
    ServletRegistrationBean h2ServletRegistration() {
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(new WebServlet());
        registrationBean.addUrlMappings("/console/*");
        registrationBean.addInitParameter("webAllowOthers", "true");
        return registrationBean;

    }
}
