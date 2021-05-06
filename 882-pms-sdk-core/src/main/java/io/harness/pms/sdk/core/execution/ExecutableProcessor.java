package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;

@OwnedBy(CDC)
@AllArgsConstructor
public class ExecutableProcessor {
  ExecuteStrategy executeStrategy;

  public void handleStart(InvokerPackage invokerPackage) {
    executeStrategy.start(invokerPackage);
  }

  public void handleResume(ResumePackage resumePackage) {
    executeStrategy.resume(resumePackage);
  }

  public void handleProgress(ProgressPackage progressPackage) {
    executeStrategy.progress(progressPackage);
  }
}
