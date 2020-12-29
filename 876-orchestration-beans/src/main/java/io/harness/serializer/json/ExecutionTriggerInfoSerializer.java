package io.harness.serializer.json;

import io.harness.pms.contracts.ambiance.ExecutionTriggerInfo;

public class ExecutionTriggerInfoSerializer extends ProtoJsonSerializer<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoSerializer() {
    super(ExecutionTriggerInfo.class);
  }
}
