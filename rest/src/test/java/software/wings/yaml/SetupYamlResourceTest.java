package software.wings.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.resources.SetupYamlResource;
import software.wings.service.intfc.AppService;
import software.wings.utils.ResourceTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * The SetupYamlResourceTest class.
 *
 * @author bsollish
 */
public class SetupYamlResourceTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  // create mocks
  private static final AppService appService = mock(AppService.class);
  private static final SetupYamlResource syr = mock(SetupYamlResource.class);

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
  private final String TEST_APP_NAME4 = "TestApp_" + TIME_IN_MS + 30;
  private final String TEST_YAML = "applications:\n- " + TEST_APP_NAME1 + "\n- " + TEST_APP_NAME2 + "\n";
  private final String TEST_YAML2 = TEST_YAML + "- " + TEST_APP_NAME3 + "\n";
  private final YamlPayload TEST_YP = new YamlPayload(TEST_YAML2);
  // adds TEST_APP_NAME4 to TEST_YAML2
  private final YamlPayload TEST_YP2 = new YamlPayload(TEST_YAML2 + "- " + TEST_APP_NAME4 + "\n");
  // adds TEST_APP_NAME4 to, and removes TEST_APP_NAME3 from TEST_YAML2
  private final YamlPayload TEST_YP3 = new YamlPayload(TEST_YAML + "- " + TEST_APP_NAME4 + "\n");

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
  private final List<String> testApps2 =
      new ArrayList<String>(Arrays.asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME3, TEST_APP_NAME4));
  private final List<String> testApps3 =
      new ArrayList<String>(Arrays.asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME4));

  private final List<Application> testApplications =
      new ArrayList<Application>(Arrays.asList(testApp1, testApp2, testApp3));

  @Before
  public void init() {
    when(appService.getAppNamesByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApps);
    when(appService.getAppsByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApplications);

    List<String> appNames = appService.getAppNamesByAccountId(TEST_ACCOUNT_ID);
    SetupYaml setup = new SetupYaml();
    setup.setAppNames(appNames);
    when(syr.get(TEST_ACCOUNT_ID)).thenReturn(YamlHelper.getYamlRestResponse(setup, "setup.yaml"));
  }

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
    RestResponse<YamlPayload> actual = resources.client()
                                           .target("/setupYaml/" + TEST_ACCOUNT_ID)
                                           .request()
                                           .get(new GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML2);
  }

  @Test
  public void testUpdateFromYamlNoChange() {
    RestResponse<SetupYaml> actual =
        resources.client()
            .target("/setupYaml/" + TEST_ACCOUNT_ID)
            .request()
            .put(Entity.entity(TEST_YP, MediaType.APPLICATION_JSON), new GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.GENERAL_YAML_INFO);
    assertThat(rm.getMessage()).isEqualTo("No change to the Yaml.");
  }

  @Test
  public void testUpdateFromYamlAddOnly() {
    RestResponse<SetupYaml> actual =
        resources.client()
            .target("/setupYaml/" + TEST_ACCOUNT_ID)
            .request()
            .put(Entity.entity(TEST_YP2, MediaType.APPLICATION_JSON), new GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps2);
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteNotEnabled() {
    RestResponse<SetupYaml> actual =
        resources.client()
            .target("/setupYaml/" + TEST_ACCOUNT_ID)
            .request()
            .put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.NON_EMPTY_DELETIONS);
    assertThat(rm.getMessage())
        .isEqualTo("WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you want to proceed.");
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteEnabled() {
    RestResponse<SetupYaml> actual =
        resources.client()
            .target("/setupYaml/" + TEST_ACCOUNT_ID + "?deleteEnabled=true")
            .request()
            .put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps3);
  }
}
