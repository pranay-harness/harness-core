package io.harness.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class WebhookHelper {
  public String generateWebhookUrl(WebhookConfigProvider webhookConfigProvider, String accountId) {
    String webhookUrl = webhookConfigProvider.getPmsApiBaseUrl();
    if (isBlank(webhookUrl)) {
      return null;
    }

    StringBuilder urlBuilder = new StringBuilder(128);
    if (webhookUrl.endsWith("#")) {
      urlBuilder.append(webhookUrl.substring(0, webhookUrl.length() - 1));
    } else {
      urlBuilder.append(webhookUrl);
    }

    if (!urlBuilder.toString().endsWith("/")) {
      urlBuilder.append('/');
    }

    // This will change to common endpoint that will be used by all
    urlBuilder.append("webhook/trigger?accountIdentifier=").append(accountId);
    return urlBuilder.toString();
  }
}
