package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.execution.utils.AmbianceUtils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@AllArgsConstructor
@Slf4j
public class ExecutableProcessor {
  ExecuteStrategy executeStrategy;

  public void handleStart(InvokerPackage invokerPackage) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(invokerPackage.getAmbiance())) {
      log.info("Calling Start for {}", executeStrategy.getClass().getSimpleName());
      executeStrategy.start(invokerPackage);
    }
  }

  public void handleResume(ResumePackage resumePackage) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(resumePackage.getAmbiance())) {
      log.info("Calling Resume for {} Strategy", executeStrategy.getClass().getSimpleName());
      executeStrategy.resume(resumePackage);
    }
  }

  public void handleProgress(ProgressPackage progressPackage) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(progressPackage.getAmbiance())) {
      log.info("Calling Progress for {} Strategy", executeStrategy.getClass().getSimpleName());
      executeStrategy.progress(progressPackage);
    }
  }
}
