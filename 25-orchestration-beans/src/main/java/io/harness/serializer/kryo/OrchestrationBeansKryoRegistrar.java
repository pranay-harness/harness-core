package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.ambiance.LevelType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.ExecutionMode;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.plan.PlanNode;
import io.harness.plan.input.InputArgs;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.StepType;
import io.harness.state.io.StatusNotifyResponseData;

import java.time.Duration;

@OwnedBy(CDC)
public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    // Add new Classes Here
    kryo.register(Status.class, 2501);
    kryo.register(StatusNotifyResponseData.class, 2502);
    kryo.register(Ambiance.class, 2503);
    kryo.register(Level.class, 2504);
    kryo.register(LevelType.class, 2505);
    kryo.register(NodeExecution.class, 2506);
    kryo.register(PlanNode.class, 2508);
    kryo.register(StepType.class, 2509);
    kryo.register(ExecutionMode.class, 2510);
    kryo.register(AdviserObtainment.class, 2511);
    kryo.register(FacilitatorObtainment.class, 2512);
    kryo.register(AdviserType.class, 2513);
    kryo.register(FacilitatorType.class, 2514);
    kryo.register(DefaultFacilitatorParams.class, 2515);
    kryo.register(Duration.class, 2516);
    kryo.register(OutcomeInstance.class, 2517);
    kryo.register(InputArgs.class, 2518);

    // Add moved/old classes here
    // Keeping the same id for moved classes
    kryo.register(ExecutionInterruptType.class, 4000);
  }
}