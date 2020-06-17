package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.advisers.retry.RetryAdviserParameters;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutput;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.delay.DelayEventNotifyData;
import io.harness.presentation.Graph;
import io.harness.presentation.GraphVertex;
import io.harness.presentation.Subgraph;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.core.dummy.DummySectionOutcome;
import io.harness.state.core.dummy.DummySectionStepTransput;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.StringNotifyResponseData;

public class OrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(ListNotifyResponseData.class, 5133);
    kryo.register(StringNotifyResponseData.class, 5271);
    kryo.register(ExecutionStatus.class, 5136);
    kryo.register(OrchestrationWorkflowType.class, 5148);
    kryo.register(WorkflowType.class, 5025);
    kryo.register(DelegateTask.Status.class, 5004);
    kryo.register(DelegateTask.class, 5003);

    kryo.register(SweepingOutput.class, 3101);
    kryo.register(ExecutionStatusResponseData.class, 3102);
    kryo.register(RetryAdviserParameters.class, 3103);
    kryo.register(DummySectionStepTransput.class, 3104);
    kryo.register(DummySectionOutcome.class, 3105);
    kryo.register(Graph.class, 3110);
    kryo.register(GraphVertex.class, 3111);
    kryo.register(Subgraph.class, 3112);

    // Put promoted classes here and do not change the id
    kryo.register(DelayEventNotifyData.class, 7273);
  }
}
