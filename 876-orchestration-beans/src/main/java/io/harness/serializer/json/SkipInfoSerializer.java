package io.harness.serializer.json;

import io.harness.pms.contracts.execution.skip.SkipInfo;

public class SkipInfoSerializer extends ProtoJsonSerializer<SkipInfo> {
  public SkipInfoSerializer() {
    super(SkipInfo.class);
  }
}