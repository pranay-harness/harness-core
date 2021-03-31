package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.RepairActionCode;
import io.harness.beans.SweepingOutput;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class CgOrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(ExecutionStatus.class, 5136);
    kryo.register(OrchestrationWorkflowType.class, 5148);
    kryo.register(WorkflowType.class, 5025);
    kryo.register(DelegateTask.Status.class, 5004);
    kryo.register(DelegateTask.class, 5003);

    kryo.register(ExecutionStatusResponseData.class, 3102);
    kryo.register(SweepingOutput.class, 3101);
    kryo.register(RepairActionCode.class, 2528);
    kryo.register(ExecutionInterruptType.class, 4000);

    // Put promoted classes here and do not change the id
  }
}
