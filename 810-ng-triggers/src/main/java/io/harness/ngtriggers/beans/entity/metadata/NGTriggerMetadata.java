package io.harness.ngtriggers.beans.entity.metadata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("ngTriggerMetadata")
@OwnedBy(PIPELINE)
public class NGTriggerMetadata {
  BuildMetadata buildMetadata;
  WebhookMetadata webhook;
  CronMetadata cron;
}
