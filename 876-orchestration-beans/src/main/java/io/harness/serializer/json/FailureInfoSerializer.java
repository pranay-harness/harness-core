package io.harness.serializer.json;

import io.harness.pms.contracts.execution.failure.FailureInfo;

public class FailureInfoSerializer extends ProtoJsonSerializer<FailureInfo> {
  public FailureInfoSerializer() {
    super(FailureInfo.class);
  }
}
