package com.imweb.shop.coupon.drools;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    private static final String RULES_PATH = "src/main/resources/rules/";

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();

        kfs.write(RULES_PATH + "coupon_discount.drl",
                ks.getResources().newClassPathResource("rules/coupon_discount.drl"));

        KieBuilder kb = ks.newKieBuilder(kfs);
        kb.buildAll();

        Results results = kb.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("Drools rule build errors: " + results.getMessages());
        }

        return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }
}
