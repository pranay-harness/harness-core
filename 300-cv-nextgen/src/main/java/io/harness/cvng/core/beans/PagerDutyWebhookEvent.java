package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagerDutyWebhookEvent {
  PagerDutyWebhookEventDTO event;

  @Value
  @Builder
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PagerDutyWebhookEventDTO {
    @JsonProperty("event_type") String eventType;
    @JsonProperty("occurred_at") Instant triggeredAt;
    PagerDutyIncidentDTO data;
  }

  @Value
  @Builder
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PagerDutyIncidentDTO {
    String id;
    String self;
    String title;
    String status;
    String urgency;
    @JsonProperty("html_url") String htmlUrl;
    PagerDutyObject priority;
    List<PagerDutyObject> assignees;
    @JsonProperty("escalation_policy") PagerDutyObject escalationPolicy;
  }

  @Value
  @Builder
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PagerDutyObject {
    String id;
    String summary;
    @JsonProperty("html_url") String htmlUrl;
  }
}
