package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.ambiance.Ambiance;
import io.harness.state.io.ambiance.LevelExecution;

public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    // Add new Classes Here
    kryo.register(NodeExecutionStatus.class, 2501);
    kryo.register(StatusNotifyResponseData.class, 2502);
    kryo.register(Ambiance.class, 2503);
    kryo.register(LevelExecution.class, 2504);

    // Add moved/old classes here
    // Keeping the same id for moved classes
    kryo.register(ExecutionInterruptType.class, 4000);
  }
}