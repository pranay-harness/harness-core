package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class WingsAdviserRegistrar {
  public Map<AdviserType, Class<? extends Adviser>> getEngineAdvisers() {
    Map<AdviserType, Class<? extends Adviser>> engineAdvisers = new HashMap<>();
    engineAdvisers.put(HttpResponseCodeSwitchAdviser.ADVISER_TYPE, HttpResponseCodeSwitchAdviser.class);

    engineAdvisers.putAll(OrchestrationAdviserRegistrar.getEngineAdvisers());
    return engineAdvisers;
  }
}
