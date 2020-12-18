package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class WingsAdviserRegistrar {
  public Map<AdviserType, Adviser> getEngineAdvisers(Injector injector) {
    Map<AdviserType, Adviser> engineAdvisers = new HashMap<>();
    engineAdvisers.put(
        HttpResponseCodeSwitchAdviser.ADVISER_TYPE, injector.getInstance(HttpResponseCodeSwitchAdviser.class));

    engineAdvisers.putAll(OrchestrationAdviserRegistrar.getEngineAdvisers(injector));
    return engineAdvisers;
  }
}
