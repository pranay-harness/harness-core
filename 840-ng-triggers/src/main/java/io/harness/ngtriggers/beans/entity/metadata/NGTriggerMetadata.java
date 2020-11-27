package io.harness.ngtriggers.beans.entity.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("ngTriggerMetadata")
public class NGTriggerMetadata {
  ArtifactMetadata artifact;
  WebhookMetadata webhook;
}
