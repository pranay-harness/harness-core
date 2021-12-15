package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionFeatureRestrictionInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionFeatureRestrictionInfoKryoSerializer
    extends ProtobufKryoSerializer<ExecutionFeatureRestrictionInfo> {
  private static ExecutionFeatureRestrictionInfoKryoSerializer instance;

  private ExecutionFeatureRestrictionInfoKryoSerializer() {}

  public static synchronized ExecutionFeatureRestrictionInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new ExecutionFeatureRestrictionInfoKryoSerializer();
    }
    return instance;
  }
}
