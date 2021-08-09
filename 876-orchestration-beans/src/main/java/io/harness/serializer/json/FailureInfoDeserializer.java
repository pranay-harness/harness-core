package io.harness.serializer.json;

import io.harness.pms.contracts.execution.failure.FailureInfo;

public class FailureInfoDeserializer extends ProtoJsonDeserializer<FailureInfo> {
  public FailureInfoDeserializer() {
    super(FailureInfo.class);
  }
}
