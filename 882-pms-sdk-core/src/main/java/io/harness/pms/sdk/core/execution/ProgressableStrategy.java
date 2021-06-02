package io.harness.pms.sdk.core.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Progressable;
import io.harness.tasks.ProgressData;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ProgressableStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  public void progress(ProgressPackage progressPackage) {
    NodeExecutionProto nodeExecutionProto = progressPackage.getNodeExecution();
    Step<?> step = extractStep(nodeExecutionProto);
    if (step instanceof Progressable) {
      ProgressData progressData = ((Progressable) step)
                                      .handleProgress(nodeExecutionProto.getAmbiance(),
                                          sdkNodeExecutionService.extractResolvedStepParameters(nodeExecutionProto),
                                          progressPackage.getProgressData());
      sdkNodeExecutionService.handleProgressResponse(nodeExecutionProto, progressData);
      return;
    }
    throw new UnsupportedOperationException("Progress Update not supported for strategy: " + this.getClass().getName());
  }
}
