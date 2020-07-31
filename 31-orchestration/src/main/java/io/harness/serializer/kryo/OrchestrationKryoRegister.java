package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.advisers.retry.RetryAdviserParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.delay.DelayEventNotifyData;
import io.harness.presentation.Graph;
import io.harness.presentation.GraphVertex;
import io.harness.presentation.Subgraph;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.core.dummy.DummySectionOutcome;
import io.harness.state.core.dummy.DummySectionStepParameters;
import io.harness.state.core.dummy.DummySectionStepTransput;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.core.section.chain.SectionChainStepParameters;

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
    kryo.register(RetryAdviserParameters.class, 3103);
    kryo.register(DummySectionStepTransput.class, 3104);
    kryo.register(DummySectionOutcome.class, 3105);
    kryo.register(Graph.class, 3110);
    kryo.register(GraphVertex.class, 3111);
    kryo.register(Subgraph.class, 3112);

    kryo.register(ForkStepParameters.class, 3113);
    kryo.register(SectionStepParameters.class, 3114);
    kryo.register(DummySectionStepParameters.class, 3115);
    kryo.register(SectionChainStepParameters.class, 3116);

    // Put promoted classes here and do not change the id
    kryo.register(DelayEventNotifyData.class, 7273);
  }
}
