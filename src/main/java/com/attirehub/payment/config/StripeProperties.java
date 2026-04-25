package com.attirehub.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    /**
     * Secret API key; when blank, Stripe checkout is disabled.
     */
    private String secretKey = "";

    /**
     * Webhook signing secret from the Stripe dashboard.
     */
    private String webhookSecret = "";
}
