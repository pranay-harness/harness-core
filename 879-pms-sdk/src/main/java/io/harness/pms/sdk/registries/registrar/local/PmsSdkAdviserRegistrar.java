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
import io.harness.pms.sdk.core.adviser.retry.RetryAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)

@UtilityClass
public class PmsSdkAdviserRegistrar {
  public Map<AdviserType, Adviser> getEngineAdvisers(Injector injector) {
    Map<AdviserType, Adviser> engineAdvisers = new HashMap<>();

    engineAdvisers.put(IgnoreAdviser.ADVISER_TYPE, injector.getInstance(IgnoreAdviser.class));
    engineAdvisers.put(OnSuccessAdviser.ADVISER_TYPE, injector.getInstance(OnSuccessAdviser.class));
    engineAdvisers.put(OnFailAdviser.ADVISER_TYPE, injector.getInstance(OnFailAdviser.class));
    engineAdvisers.put(ManualInterventionAdviser.ADVISER_TYPE, injector.getInstance(ManualInterventionAdviser.class));
    engineAdvisers.put(OnAbortAdviser.ADVISER_TYPE, injector.getInstance(OnAbortAdviser.class));
    engineAdvisers.put(OnMarkSuccessAdviser.ADVISER_TYPE, injector.getInstance(OnMarkSuccessAdviser.class));
    engineAdvisers.put(RetryAdviser.ADVISER_TYPE, injector.getInstance(RetryAdviser.class));

    return engineAdvisers;
  }
}
