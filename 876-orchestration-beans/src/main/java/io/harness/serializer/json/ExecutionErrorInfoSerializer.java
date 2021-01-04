package io.harness.serializer.json;

import io.harness.pms.contracts.execution.ExecutionErrorInfo;

public class ExecutionErrorInfoSerializer extends ProtoJsonSerializer<ExecutionErrorInfo> {
  public ExecutionErrorInfoSerializer() {
    super(ExecutionErrorInfo.class);
  }
}
