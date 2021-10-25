package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.iterator.IteratorConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class PipelineServiceIteratorsConfig {
  @JsonProperty("webhook") IteratorConfig triggerWebhookConfig;
  @JsonProperty("scheduledTrigger") IteratorConfig scheduleTriggerConfig;
  @JsonProperty("timeoutEngine") IteratorConfig timeoutEngineConfig;
  @JsonProperty("barrier") IteratorConfig barrierConfig;
  @JsonProperty("approvalInstance") IteratorConfig approvalInstanceConfig;
  @JsonProperty("resourceRestraint") IteratorConfig resourceRestraintConfig;
  @JsonProperty("interruptMonitor") IteratorConfig interruptMonitorConfig;
}
