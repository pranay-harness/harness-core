package software.wings;

import static software.wings.rules.WingsRule.TestType.INTEGRATION;

import org.junit.Rule;
import software.wings.rules.WingsRule;

/**
 * Created by anubhaw on 4/28/16.
 */

public class WingsBaseIntegrationTest extends WingsBaseTest {
  @Rule public WingsRule wingsRule = new WingsRule(INTEGRATION);
}
