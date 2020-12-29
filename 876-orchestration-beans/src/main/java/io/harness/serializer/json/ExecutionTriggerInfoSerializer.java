package io.harness.serializer.json;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;

public class ExecutionTriggerInfoSerializer extends ProtoJsonSerializer<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoSerializer() {
    super(ExecutionTriggerInfo.class);
  }
}
