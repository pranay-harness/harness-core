package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;

import lombok.AllArgsConstructor;

@OwnedBy(CDC)
@Redesign
@AllArgsConstructor
public class ExecutableProcessor {
  ExecuteStrategy executeStrategy;

  public void handleStart(InvokerPackage invokerPackage) {
    executeStrategy.start(invokerPackage);
  }

  public void handleResume(ResumePackage resumePackage) {
    executeStrategy.resume(resumePackage);
  }
}
