package software.wings.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.resources.SetupYamlResource;
import software.wings.service.intfc.AppService;
import software.wings.utils.ResourceTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;

/**
 * The SetupYamlResourceTest class.
 *
 * @author bsollish
 */
public class SetupYamlResourceTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final AppService appService = mock(AppService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new SetupYamlResource(appService)).build();
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID-" + TIME_IN_MS;
  private final String TEST_APP1 = "TEST-APP-" + TIME_IN_MS;
  private final String TEST_APP2 = "TEST-APP-" + TIME_IN_MS + 10;
  private final String TEST_APP3 = "TEST-APP-" + TIME_IN_MS + 20;
  private final String TEST_APP_NAME1 = "TestApp_" + TIME_IN_MS;
  private final String TEST_APP_NAME2 = "TestApp_" + TIME_IN_MS + 10;
  private final String TEST_APP_NAME3 = "TestApp_" + TIME_IN_MS + 20;
  private final String TEST_YAML =
      "applications:\n- " + TEST_APP_NAME1 + "\n- " + TEST_APP_NAME2 + "\n- " + TEST_APP_NAME3 + "\n";

  private final Application testApp1 = anApplication()
                                           .withUuid(TEST_APP1)
                                           .withAccountId(TEST_ACCOUNT_ID)
                                           .withAppId(TEST_APP1)
                                           .withName(TEST_APP_NAME1)
                                           .build();
  private final Application testApp2 = anApplication()
                                           .withUuid(TEST_APP2)
                                           .withAccountId(TEST_ACCOUNT_ID)
                                           .withAppId(TEST_APP2)
                                           .withName(TEST_APP_NAME2)
                                           .build();
  private final Application testApp3 = anApplication()
                                           .withUuid(TEST_APP3)
                                           .withAccountId(TEST_ACCOUNT_ID)
                                           .withAppId(TEST_APP3)
                                           .withName(TEST_APP_NAME3)
                                           .build();

  private final List<String> testApps =
      new ArrayList<String>(Arrays.asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME3));

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService);
  }

  @Test
  public void testGetYaml() {
    when(appService.getAppNamesByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApps);
    RestResponse<YamlPayload> actual = resources.client()
                                           .target("/setupYaml/" + TEST_ACCOUNT_ID)
                                           .request()
                                           .get(new GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML);
  }

  /*
  @Test
  public void testUpdateFromYaml() {
    YamlPayload yp = new YamlPayload(TEST_YAML);

    // when we save ANY instance of Application, we want to return testApp2
    when(appService.save(Mockito.any(Application.class))).thenReturn(testApp2);
    RestResponse<Application> actual = resources.client().target("/apps/yaml?accountId=" +
  TEST_ACCOUNT_ID).request().post(Entity.entity(yp, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<Application>>() {}); Application app = actual.getResource();

    assertThat(app.getName()).isEqualTo(TEST_NAME);
    assertThat(app.getDescription()).isEqualTo(TEST_DESCRIPTION);
  }
  */
}
