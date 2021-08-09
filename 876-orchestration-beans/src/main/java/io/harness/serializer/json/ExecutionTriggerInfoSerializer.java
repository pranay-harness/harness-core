package io.harness.serializer.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;

@OwnedBy(PIPELINE)
public class ExecutionTriggerInfoSerializer extends ProtoJsonSerializer<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoSerializer() {
    super(ExecutionTriggerInfo.class);
  }
}
