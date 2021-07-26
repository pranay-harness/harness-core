package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeRunInfoKryoSerializer extends ProtobufKryoSerializer<NodeRunInfo> {
  private static NodeRunInfoKryoSerializer instance;

  public NodeRunInfoKryoSerializer() {}

  public static synchronized NodeRunInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new NodeRunInfoKryoSerializer();
    }
    return instance;
  }
}
