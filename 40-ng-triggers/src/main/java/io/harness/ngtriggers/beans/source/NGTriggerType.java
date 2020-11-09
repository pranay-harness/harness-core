package io.harness.ngtriggers.beans.source;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum NGTriggerType {
  @JsonProperty("Webhook") WEBHOOK,
  @JsonProperty("NewArtifact") NEW_ARTIFACT,
  @JsonProperty("Scheduled") SCHEDULED
}
