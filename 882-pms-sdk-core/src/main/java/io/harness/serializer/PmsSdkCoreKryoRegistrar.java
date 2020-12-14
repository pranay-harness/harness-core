package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviserParameters;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviserParameters;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviserParameters;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviserParameters;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviserParameters;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviserParameters;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.facilitator.DefaultFacilitatorParams;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class PmsSdkCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DefaultFacilitatorParams.class, 2515);
    kryo.register(StepOutcome.class, 2521);
    kryo.register(SweepingOutput.class, 3101);
    kryo.register(PassThroughData.class, 2535);

    kryo.register(RetryAdviserParameters.class, 3103);
    kryo.register(OnSuccessAdviserParameters.class, 3104);
    kryo.register(OnFailAdviserParameters.class, 3105);
    kryo.register(IgnoreAdviserParameters.class, 3106);
    kryo.register(ManualInterventionAdviserParameters.class, 3107);
    kryo.register(OnMarkSuccessAdviserParameters.class, 3108);
    kryo.register(OnAbortAdviserParameters.class, 3109);

    // New classes here
    kryo.register(PlanNode.class, 88201);
  }
}
