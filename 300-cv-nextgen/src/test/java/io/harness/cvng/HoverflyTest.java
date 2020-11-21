package io.harness.cvng;

import io.harness.CategoryTest;

import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.ClassRule;
@Slf4j
public class HoverflyTest extends CategoryTest {
  //  TODO: We need make capture and simulation switching easier.
  // TODO: trying out if assigning a fixed port fixes the CI issues. We need a common rule for all tests.

  @ClassRule public static HoverflyRule HOVERFLY_RULE;
  static {
    try {
      HOVERFLY_RULE = HoverflyRule.inSimulationMode(HoverflyConfig.localConfigs().disableTlsVerification());
      // HOVERFLY_RULE = HoverflyRule.inCaptureMode(HoverflyConfig.localConfigs().disableTlsVerification());
    } catch (Exception e) {
      log.info("Initializing HoverflyRule inSimulationMode failed. Retrying one more time : {}", e);
      // This is rarely failing in CI with port conflict exception. So retrying one more time.
      // If you still face this issue in your PR's please notify me(kamal).
      HOVERFLY_RULE = HoverflyRule.inSimulationMode(HoverflyConfig.localConfigs().disableTlsVerification());
    }
  }
}
