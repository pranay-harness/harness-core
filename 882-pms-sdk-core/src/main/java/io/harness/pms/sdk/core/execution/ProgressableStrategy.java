package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Progressable;
import io.harness.tasks.ProgressData;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ProgressableStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  public void progress(ProgressPackage progressPackage) {
    Ambiance ambiance = progressPackage.getAmbiance();
    Step<?> step = extractStep(ambiance);
    if (step instanceof Progressable) {
      ProgressData progressData = ((Progressable) step)
                                      .handleProgress(progressPackage.getAmbiance(),
                                          progressPackage.getStepParameters(), progressPackage.getProgressData());
      sdkNodeExecutionService.handleProgressResponse(ambiance, progressData);
      return;
    }
    throw new UnsupportedOperationException("Progress Update not supported for strategy: " + this.getClass().getName());
  }
}
