package software.wings.yaml;

import org.junit.Before;
import software.wings.integration.BaseIntegrationTest;

/**
 * Created by bsollish on 8/10/17.
 */
public class YamlPayloadIntegrationTest extends BaseIntegrationTest {
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_NAME_POST = "TestAppPOST_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_POST = "stuffPOST";
  private final String TEST_YAML_POST =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_POST + "\ndescription: " + TEST_DESCRIPTION_POST;
  private final String TEST_NAME_PUT = "TestAppPUT_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_PUT = "stuffPUT";
  private final String TEST_YAML_PUT =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_PUT + "\ndescription: " + TEST_DESCRIPTION_PUT;

  @Override
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
  }
}
