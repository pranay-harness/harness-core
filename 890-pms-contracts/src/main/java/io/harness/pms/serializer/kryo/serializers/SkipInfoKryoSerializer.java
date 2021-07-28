package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class SkipInfoKryoSerializer extends ProtobufKryoSerializer<SkipInfo> {
  private static SkipInfoKryoSerializer instance;

  private SkipInfoKryoSerializer() {}

  public static synchronized SkipInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new SkipInfoKryoSerializer();
    }
    return instance;
  }
}