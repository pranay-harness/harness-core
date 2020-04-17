package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StatusNotifyResponseData;

public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(NodeExecutionStatus.class, 2501);
    kryo.register(StatusNotifyResponseData.class, 2502);
  }
}