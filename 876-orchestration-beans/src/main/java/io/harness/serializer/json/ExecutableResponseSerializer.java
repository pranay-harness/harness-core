package io.harness.serializer.json;

import io.harness.pms.contracts.execution.ExecutableResponse;

public class ExecutableResponseSerializer extends ProtoJsonSerializer<ExecutableResponse> {
  public ExecutableResponseSerializer(Class<ExecutableResponse> t) {
    super(t);
  }

  public ExecutableResponseSerializer() {
    this(null);
  }
}
