package io.harness.pms.sdk.core.supporter.sync;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class TestSyncStep implements SyncExecutable<TestSyncStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType("TEST_SYNC").setStepCategory(StepCategory.STEP).build();

  @Override
  public Class<TestSyncStepParameters> getStepParametersClass() {
    return TestSyncStepParameters.class;
  }

  @Override
  public StepResponse executeSync(Ambiance ambiance, TestSyncStepParameters dummyStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("Dummy Step getting executed. Identifier: {}",
        Preconditions.checkNotNull(AmbianceUtils.obtainCurrentLevel(ambiance)).getIdentifier());
    return StepResponse.builder().status(Status.SUCCEEDED).build();
  }
}
