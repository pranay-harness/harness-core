package io.harness.cdng.pipeline.executions.beans;

import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.executions.TriggerType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionTriggerInfo {
  EmbeddedUser triggeredBy;
  TriggerType triggerType;
}
