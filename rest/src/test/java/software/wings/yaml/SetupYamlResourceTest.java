package software.wings.yaml;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static software.wings.beans.Application.Builder.anApplication;

import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;

import java.util.List;

// import software.wings.resources.yaml.SetupYamlResource;

/**
 * The SetupYamlResourceTest class.
 *
 * @author bsollish
 */
public class SetupYamlResourceTest {
  private static final Logger logger = LoggerFactory.getLogger(SetupYamlResourceTest.class);

  // create mocks
  private static final AppService appService = mock(AppService.class);
  private static final SettingsService settingsService = mock(SettingsService.class);
  private static final YamlHistoryService yamlHistoryService = mock(YamlHistoryService.class);
  private static final YamlGitService yamlGitSyncService = mock(YamlGitService.class);
  // private static final SetupYamlResource syr = mock(SetupYamlResource.class);

  // The constant resources.
  // @ClassRule public static final ResourceTestRule resources = ResourceTestRule.builder().addResource(new
  // SetupYamlResource(appService, settingsService, yamlHistoryService, yamlGitSyncService)).build();

  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID-" + TIME_IN_MS;
  private final String TEST_APP1 = "TEST-APP-" + TIME_IN_MS;
  private final String TEST_APP2 = "TEST-APP-" + TIME_IN_MS + 10;
  private final String TEST_APP3 = "TEST-APP-" + TIME_IN_MS + 20;
  private final String TEST_APP_NAME1 = "TestApp_" + TIME_IN_MS;
  private final String TEST_APP_NAME2 = "TestApp_" + TIME_IN_MS + 10;
  private final String TEST_APP_NAME3 = "TestApp_" + TIME_IN_MS + 20;
  private final String TEST_APP_NAME4 = "TestApp_" + TIME_IN_MS + 30;
  private final String TEST_YAML1 = "applications:\n  - " + TEST_APP_NAME1 + "\n  - " + TEST_APP_NAME2 + "\n";
  private final String TEST_YAML2 = TEST_YAML1 + "  - " + TEST_APP_NAME3 + "\n";

  private final YamlPayload TEST_YP = new YamlPayload(TEST_YAML2);
  // adds TEST_APP_NAME4 to TEST_YAML2
  private final YamlPayload TEST_YP2 = new YamlPayload(TEST_YAML2 + "  - " + TEST_APP_NAME4 + "\n");
  // adds TEST_APP_NAME4 to, and removes TEST_APP_NAME3 from TEST_YAML2
  private final YamlPayload TEST_YP3 = new YamlPayload(TEST_YAML1 + "  - " + TEST_APP_NAME4 + "\n");

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

  private final List<String> testApps1 = asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME3);
  private final List<String> testApps2 = asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME3, TEST_APP_NAME4);
  private final List<String> testApps3 = asList(TEST_APP_NAME1, TEST_APP_NAME2, TEST_APP_NAME4);

  private final List<Application> testApplications = asList(testApp1, testApp2, testApp3);

  //=============================================================================================================
  // TODO - these tests (or their equivalent) need to be rewritten given the extensive refactoring that was done
  //=============================================================================================================

  /*
  @Before
  public void init() {
    when(appService.getAppNamesByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApps1);
    when(appService.getAppsByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApplications);

    List<String> appNames = appService.getAppNamesByAccountId(TEST_ACCOUNT_ID);
    SetupYaml setup = new SetupYaml();
    setup.setAppNames(appNames);
    when(syr.get(TEST_ACCOUNT_ID)).thenReturn(YamlHelper.getYamlRestResponse(yamlGitSyncService, TEST_ACCOUNT_ID, setup,
  "setup.yaml"));
  }
  */

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService);
  }

  /*
  @Test
  public void testGetYaml() {
    RestResponse<YamlPayload> actual = resources.client().target("/setupYaml/" + TEST_ACCOUNT_ID).request().get(new
  GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML2);
  }
  */

  /*
  @Test
  public void testUpdateFromYamlNoChange() {
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" +
  TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.GENERAL_YAML_INFO);
    assertThat(rm.getMessage()).isEqualTo("No change to the Yaml.");
  }
  */

  /*
  @Test
  public void testUpdateFromYamlAddOnly() {
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" +
  TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP2, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps2);
  }
  */

  /*
  @Test
  public void testUpdateFromYamlAddAndDeleteNotEnabled() {
    RestResponse<SetupYaml>
        actual = resources.client().target("/setupYaml/" + TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP3,
  MediaType.APPLICATION_JSON), new GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.NON_EMPTY_DELETIONS);
    assertThat(rm.getMessage()).isEqualTo("WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you
  want to proceed.");
  }
  */

  /*
  @Test
  public void testUpdateFromYamlAddAndDeleteEnabled() {
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" + TEST_ACCOUNT_ID +
  "?deleteEnabled=true").request().put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps3);
  }
  */
}
