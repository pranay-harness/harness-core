package io.harness.ngtriggers.beans.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ngTriggerType")
public enum NGTriggerType {
  @JsonProperty("Webhook") WEBHOOK,
  @JsonProperty("New Artifact") NEW_ARTIFACT,
  @JsonProperty("Scheduled") SCHEDULED
}
