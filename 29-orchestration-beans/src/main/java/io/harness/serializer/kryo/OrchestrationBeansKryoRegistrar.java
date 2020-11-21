package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance;
import io.harness.data.SweepingOutput;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;
import io.harness.interrupts.RepairActionCode;
import io.harness.plan.PlanNode;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.TaskMode;

import com.esotericsoftware.kryo.Kryo;
import java.time.Duration;

/**
 * We are trying to remain as independent from Kryo as possible.
 * All the classes which get saved inside DelegateResponseData need to be registered as our
 * WaitNotify engine used that.
 */
@OwnedBy(CDC)
public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // Add new Classes Here
    kryo.register(StatusNotifyResponseData.class, 2502);
    kryo.register(Ambiance.class, 2503);
    kryo.register(NodeExecution.class, 2506);
    kryo.register(PlanNode.class, 2508);
    kryo.register(DefaultFacilitatorParams.class, 2515);
    kryo.register(Duration.class, 2516);
    kryo.register(OutcomeInstance.class, 2517);
    kryo.register(StepResponseNotifyData.class, 2519);
    kryo.register(FailureInfo.class, 2520);
    kryo.register(StepOutcome.class, 2521);

    kryo.register(AsyncExecutableResponse.class, 2522);
    kryo.register(ChildExecutableResponse.class, 2523);
    kryo.register(ChildrenExecutableResponse.class, 2524);
    kryo.register(ChildChainResponse.class, 2525);
    kryo.register(TaskExecutableResponse.class, 2526);
    kryo.register(TaskChainExecutableResponse.class, 2527);
    kryo.register(RepairActionCode.class, 2528);

    kryo.register(StepOutcomeRef.class, 2529);

    kryo.register(TaskMode.class, 2532);
    kryo.register(InterruptEffect.class, 2534);

    // Add moved/old classes here
    // Keeping the same id for moved classes
    kryo.register(ExecutionInterruptType.class, 4000);
    kryo.register(SweepingOutput.class, 3101);
  }
}
