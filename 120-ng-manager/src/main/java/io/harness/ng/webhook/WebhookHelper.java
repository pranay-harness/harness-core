package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.constants.Constants.X_AMZ_SNS_MESSAGE_TYPE;
import static io.harness.constants.Constants.X_BIT_BUCKET_EVENT;
import static io.harness.constants.Constants.X_GIT_HUB_EVENT;
import static io.harness.constants.Constants.X_GIT_LAB_EVENT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType.AWS_CODECOMMIT;
import static io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType.BITBUCKET;
import static io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType.GITHUB;
import static io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType.GITLAB;
import static io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType.UNRECOGNIZED;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookEventType.ISSUE_COMMENT;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookEventType.PR;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookEventType.PUSH;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookTriggerType.CUSTOM;
import static io.harness.eventsframework.webhookpayloads.webhookdata.WebhookTriggerType.GIT;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.eventsframework.webhookpayloads.webhookdata.EventHeader;
import io.harness.eventsframework.webhookpayloads.webhookdata.GitDetails;
import io.harness.eventsframework.webhookpayloads.webhookdata.SourceRepoType;
import io.harness.eventsframework.webhookpayloads.webhookdata.WebhookDTO;
import io.harness.ng.webhook.entities.WebhookEvent;
import io.harness.ng.webhook.entities.WebhookEvent.WebhookEventBuilder;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class WebhookHelper {
  public WebhookEvent toNGTriggerWebhookEvent(String accountIdentifier, String payload, HttpHeaders httpHeaders) {
    List<HeaderConfig> headerConfigs = new ArrayList<>();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> headerConfigs.add(HeaderConfig.builder().key(k).values(v).build()));

    WebhookEventBuilder webhookEventBuilder =
        WebhookEvent.builder().accountId(accountIdentifier).headers(headerConfigs).payload(payload);

    return webhookEventBuilder.build();
  }

  public boolean containsHeaderKey(Map<String, List<String>> headers, String key) {
    Set<String> headerKeys = headers.keySet();
    if (isEmpty(headerKeys) || isBlank(key)) {
      return false;
    }

    return headerKeys.contains(key) || headerKeys.contains(key.toLowerCase())
        || headerKeys.stream().anyMatch(key::equalsIgnoreCase);
  }

  public WebhookDTO generateWebhookDTO(
      WebhookEvent event, ParseWebhookResponse parseWebhookResponse, SourceRepoType sourceRepoType) {
    WebhookDTO.Builder builder = WebhookDTO.newBuilder()
                                     .setJsonPayload(event.getPayload())
                                     .addAllHeaders(generateEventHeaders(event))
                                     .setParsedResponse(parseWebhookResponse)
                                     .setAccountId(event.getAccountId())
                                     .setEventId(event.getUuid())
                                     .setTime(event.getCreatedAt());

    if (parseWebhookResponse == null) {
      builder.setWebhookTriggerType(CUSTOM);
    } else {
      builder.setWebhookTriggerType(GIT);
      builder.setGitDetails(generateGitDetails(parseWebhookResponse, sourceRepoType));
    }

    return builder.build();
  }

  private List<EventHeader> generateEventHeaders(WebhookEvent event) {
    return event.getHeaders()
        .stream()
        .map(headerConfig
            -> EventHeader.newBuilder().setKey(headerConfig.getKey()).addAllValues(headerConfig.getValues()).build())
        .collect(toList());
  }

  public GitDetails generateGitDetails(ParseWebhookResponse parseWebhookResponse, SourceRepoType sourceRepoType) {
    GitDetails.Builder builder = GitDetails.newBuilder().setSourceRepoType(sourceRepoType);
    if (parseWebhookResponse.hasPush()) {
      builder.setEvent(PUSH);
    } else if (parseWebhookResponse.hasPr()) {
      builder.setEvent(PR);
    } else if (parseWebhookResponse.hasComment()) {
      builder.setEvent(ISSUE_COMMENT);
    }

    return builder.build();
  }

  public SourceRepoType getSourceRepoType(WebhookEvent event) {
    Map<String, List<String>> headers =
        event.getHeaders().stream().collect(Collectors.toMap(HeaderConfig::getKey, HeaderConfig::getValues));

    SourceRepoType sourceRepoType = UNRECOGNIZED;
    if (containsHeaderKey(headers, X_GIT_HUB_EVENT)) {
      sourceRepoType = GITHUB;
    } else if (containsHeaderKey(headers, X_GIT_LAB_EVENT)) {
      sourceRepoType = GITLAB;
    } else if (containsHeaderKey(headers, X_BIT_BUCKET_EVENT)) {
      sourceRepoType = BITBUCKET;
    } else if (containsHeaderKey(headers, X_AMZ_SNS_MESSAGE_TYPE)) {
      sourceRepoType = AWS_CODECOMMIT;
    }

    return sourceRepoType;
  }
}
