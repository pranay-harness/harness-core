package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

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
