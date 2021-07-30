package io.harness.ngtriggers.beans.source;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ngTriggerType")
@OwnedBy(PIPELINE)
public enum NGTriggerType {
  @JsonProperty("Webhook") WEBHOOK,
  @JsonProperty("Artifact") ARTIFACT,
  @JsonProperty("Manifest") MANIFEST,
  @JsonProperty("Scheduled") SCHEDULED
}
