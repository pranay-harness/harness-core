package io.harness.pms.sdk.registries.registrar.local;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviser;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviser;
import io.harness.pms.sdk.core.adviser.marksuccess.OnMarkSuccessAdviser;
import io.harness.pms.sdk.core.adviser.nextstep.NextStepAdviser;
import io.harness.pms.sdk.core.adviser.retry.RetryAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)

@UtilityClass
public class PmsSdkAdviserRegistrar {
  public Map<AdviserType, Class<? extends Adviser>> getEngineAdvisers() {
    Map<AdviserType, Class<? extends Adviser>> engineAdvisers = new HashMap<>();

    engineAdvisers.put(IgnoreAdviser.ADVISER_TYPE, IgnoreAdviser.class);
    engineAdvisers.put(OnSuccessAdviser.ADVISER_TYPE, OnSuccessAdviser.class);
    engineAdvisers.put(NextStepAdviser.ADVISER_TYPE, NextStepAdviser.class);
    engineAdvisers.put(OnFailAdviser.ADVISER_TYPE, OnFailAdviser.class);
    engineAdvisers.put(ManualInterventionAdviser.ADVISER_TYPE, ManualInterventionAdviser.class);
    engineAdvisers.put(OnAbortAdviser.ADVISER_TYPE, OnAbortAdviser.class);
    engineAdvisers.put(OnMarkSuccessAdviser.ADVISER_TYPE, OnMarkSuccessAdviser.class);
    engineAdvisers.put(RetryAdviser.ADVISER_TYPE, RetryAdviser.class);

    return engineAdvisers;
  }
}
