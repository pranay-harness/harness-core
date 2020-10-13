package io.harness.beans.steps;

import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_LITE_ENGINE;
import static io.harness.beans.steps.CIStepInfoType.CIStepExecEnvironment.CI_MANAGER;

import lombok.Getter;

public enum CIStepInfoType {
  BUILD(CI_LITE_ENGINE),
  TEST(CI_LITE_ENGINE),
  SETUP_ENV(CI_MANAGER),
  CLEANUP(CI_MANAGER),
  PUBLISH(CI_LITE_ENGINE),
  RUN(CI_LITE_ENGINE),
  PLUGIN(CI_LITE_ENGINE),
  GIT_CLONE(CI_LITE_ENGINE),
  LITE_ENGINE_TASK(CI_LITE_ENGINE),
  SAVE_CACHE(CI_LITE_ENGINE),
  RESTORE_CACHE(CI_LITE_ENGINE);

  @Getter private CIStepExecEnvironment ciStepExecEnvironment;

  CIStepInfoType(CIStepExecEnvironment ciStepExecEnvironment) {
    this.ciStepExecEnvironment = ciStepExecEnvironment;
  }
  public enum CIStepExecEnvironment { CI_MANAGER, CI_LITE_ENGINE }
}
