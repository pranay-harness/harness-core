package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.data.StepTransput;
import io.harness.pms.sdk.core.execution.AsyncSdkProgressCallback;
import io.harness.pms.sdk.core.execution.AsyncSdkResumeCallback;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.PipelineViewObject;

import java.util.Set;

@OwnedBy(PIPELINE)
public class PmsSdkCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Outcome.class);
    set.add(StepTransput.class);
    set.add(PassThroughData.class);
    set.add(PipelineViewObject.class);
    set.add(ExecutionSweepingOutput.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("pms.sdk.core.execution.AsyncSdkResumeCallback", AsyncSdkResumeCallback.class);
    h.put("pms.sdk.core.execution.AsyncSdkProgressCallback", AsyncSdkProgressCallback.class);
  }
}
