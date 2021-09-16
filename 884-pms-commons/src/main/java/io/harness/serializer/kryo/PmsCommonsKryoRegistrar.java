package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.OrchestrationMap;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.data.output.PmsSweepingOutput;
import io.harness.pms.data.stepdetails.PmsStepDetails;
import io.harness.pms.data.stepparameters.PmsStepParameters;
import io.harness.pms.execution.facilitator.DefaultFacilitatorParams;
import io.harness.pms.timeout.AbsoluteSdkTimeoutTrackerParameters;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsCommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // keeping ids same
    kryo.register(DefaultFacilitatorParams.class, 2515);

    kryo.register(OrchestrationMap.class, 88401);
    kryo.register(PmsOutcome.class, 88402);
    kryo.register(PmsSweepingOutput.class, 88403);
    kryo.register(AbsoluteSdkTimeoutTrackerParameters.class, 88404);
    kryo.register(PmsStepParameters.class, 88405);
    kryo.register(PmsStepDetails.class, 88406);
  }
}
