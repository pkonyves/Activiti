package org.activiti.spring.boot;

import org.activiti.engine.ProcessEngine;
import org.activiti.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.*;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Ignore
public class EndpointAutoConfigurationTest {

    private AnnotationConfigEmbeddedWebApplicationContext applicationContext;

    @Configuration
    @Import({EmbeddedServletContainerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            ServerPropertiesAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            WebMvcAutoConfiguration.class})
    public static class EmbeddedContainerConfiguration {
    }

    @Configuration
    @Import({DataSourceAutoConfiguration.class,
            MetricFilterAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
            ManagementServerPropertiesAutoConfiguration.class,
            MetricRepositoryAutoConfiguration.class,
            DataSourceProcessEngineAutoConfiguration.class, EndpointAutoConfiguration.class})
    public static class EndpointConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @After
    public void close() {
        this.applicationContext.close();
    }

    @Test
    public void mvcEndpoint() throws Throwable {

        this.applicationContext = new AnnotationConfigEmbeddedWebApplicationContext();
        this.applicationContext.register(EmbeddedContainerConfiguration.class, EndpointConfiguration.class);
        this.applicationContext.refresh();

        ProcessEngine processEngine = this.applicationContext.getBean(ProcessEngine.class);
        org.junit.Assert.assertNotNull("the processEngine should not be null", processEngine);

        ProcessEngineEndpoint processEngineEndpoint =
                this.applicationContext.getBean(ProcessEngineEndpoint.class);
        org.junit.Assert.assertNotNull("the processEngineEndpoint should not be null", processEngineEndpoint);

        RestTemplate restTemplate = this.applicationContext.getBean(RestTemplate.class);

        ResponseEntity<Map> mapResponseEntity =
                restTemplate.getForEntity("http://localhost:8080/activiti/", Map.class);

        Map map = mapResponseEntity.getBody();

        String[] criticalKeys = {"completedTaskCount", "openTaskCount", "cachedProcessDefinitionCount"};

        Map<?, ?> invokedResults = processEngineEndpoint.invoke();
        for (String k : criticalKeys) {
            org.junit.Assert.assertTrue(map.containsKey(k));
            org.junit.Assert.assertEquals(((Number) map.get(k)).longValue(), ((Number) invokedResults.get(k)).longValue());
        }

    }
}
