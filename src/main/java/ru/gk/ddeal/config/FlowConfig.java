package ru.gk.ddeal.config;


import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.mail.dsl.Mail;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import ru.gk.ddeal.service.AvService;
import ru.gk.ddeal.service.DateService;
import ru.gk.ddeal.service.FiveService;

@Configuration
public class FlowConfig {
    private static final Logger log = LoggerFactory.getLogger(FlowConfig.class);

    @Autowired
    DateService dateService;

    @Autowired
    FiveService x5;

    @Autowired
    AvService av;

    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext ctx;


    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder ->
                jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(500))
                .setReadTimeout(Duration.ofSeconds(500))
                .build();
    }


    @Bean
    public IntegrationFlow imapMailFlow() throws UnsupportedEncodingException {
        if (!StringUtils.hasLength(env.getProperty("inbox"))) {
            log.error("No mail inbox defined!");
            System.exit(SpringApplication.exit(ctx, () -> 0));
        }

        final String inbox = new String(env.getProperty("inbox").getBytes("ISO-8859-1"), "UTF-8");

        return IntegrationFlows
                .from(Mail.imapInboundAdapter(inbox)
                                .searchTermStrategy(dateService::dateAfterTerm).shouldMarkMessagesAsRead(false)
                                .simpleContent(true)
                                .javaMailProperties(p -> {
                                    p.put("mail.imaps.connectiontimeout", "300000");
                                    p.put("mail.imaps.timeout", "300000");
                                    p.put("mail.imaps.ssl.trust", "*");
                                    p.put("mail.imaps.ssl.checkserveridentity", "false");
                                    p.put("mail.debug", "false");
                                }),
                        e -> e.autoStartup(true)
                                .poller(p -> p.fixedDelay(5000)))
                .<javax.mail.Message>handle((payload, headers) -> (payload))
                .handle(x5, "requestHandler")
                .handle(av, "requestHandler")
                .handle(dateService, "dateHadler")
                .get();
    }

}
