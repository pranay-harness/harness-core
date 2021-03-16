package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class OrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(ExecutionStatus.class, 5136);
    kryo.register(OrchestrationWorkflowType.class, 5148);
    kryo.register(WorkflowType.class, 5025);
    kryo.register(DelegateTask.Status.class, 5004);
    kryo.register(DelegateTask.class, 5003);

    kryo.register(ExecutionStatusResponseData.class, 3102);

    // Put promoted classes here and do not change the id
  }
}
