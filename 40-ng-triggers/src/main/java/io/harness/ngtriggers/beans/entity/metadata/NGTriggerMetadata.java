package io.harness.ngtriggers.beans.entity.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NGTriggerMetadata {
  ArtifactMetadata artifact;
  WebhookMetadata webhook;
}
