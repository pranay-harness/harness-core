package io.harness.pms.pipeline;

import io.harness.beans.EmbeddedUser;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ExecutionTriggerInfo {
  EmbeddedUser triggeredBy;
  TriggerType triggerType;
}
