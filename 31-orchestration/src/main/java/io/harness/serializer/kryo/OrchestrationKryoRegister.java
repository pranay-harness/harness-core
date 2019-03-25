package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.StringNotifyResponseData;

public class OrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(ListNotifyResponseData.class, 5133);
    kryo.register(StringNotifyResponseData.class, 5271);
    kryo.register(ExecutionStatus.class, 5136);
    kryo.register(OrchestrationWorkflowType.class, 5148);
    kryo.register(WorkflowType.class, 5025);
  }
}
