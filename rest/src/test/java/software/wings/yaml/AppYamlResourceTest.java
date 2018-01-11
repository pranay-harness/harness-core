package software.wings.yaml;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Service.Builder.aService;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlHistoryService;

import java.util.List;

/**
 * The AppYamlResourceTest class.
 *
 * @author bsollish
 */
public class AppYamlResourceTest {
  private static final Logger logger = LoggerFactory.getLogger(AppYamlResourceTest.class);

  // create mocks
  private static final AppService appService = mock(AppService.class);
  private static final ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);
  private static final EnvironmentService environmentService = mock(EnvironmentService.class);
  private static final YamlHistoryService yamlHistoryService = mock(YamlHistoryService.class);
  private static final YamlGitService yamlGitSyncService = mock(YamlGitService.class);

  /**
   * The constant resources.
   */
  //@ClassRule public static final ResourceTestRule resources = ResourceTestRule.builder().addResource(new
  // AppYamlResource(appService, serviceResourceService, environmentService, yamlHistoryService,
  // yamlGitSyncService)).build();

  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID" + TIME_IN_MS;

  private final String TEST_APP_ID1 = "TEST-APP-ID" + TIME_IN_MS;
  private final String TEST_APP_NAME1 = "TEST-APP-NAME" + TIME_IN_MS;
  private final String TEST_APP_DESCRIPTION1 = "TEST-APP-DESCRIPTION" + TIME_IN_MS;

  private final String TEST_SERVICE1 = "TEST-SERVICE-" + TIME_IN_MS;
  private final String TEST_SERVICE2 = "TEST-SERVICE-" + TIME_IN_MS + 10;
  private final String TEST_SERVICE3 = "TEST-SERVICE-" + TIME_IN_MS + 20;
  private final String TEST_SERVICE4 = "TEST-SERVICE-" + TIME_IN_MS + 30;

  private final String TEST_YAML1 = "name: " + TEST_APP_NAME1 + "\ndescription: " + TEST_APP_DESCRIPTION1
      + "\nservices:\n  - " + TEST_SERVICE1 + "\n  - " + TEST_SERVICE2 + "\n";
  private final String TEST_YAML2 = TEST_YAML1 + "  - " + TEST_SERVICE3 + "\n";
  private final YamlPayload TEST_YP = new YamlPayload(TEST_YAML2);
  // adds TEST_APP_NAME4 to TEST_YAML2
  private final YamlPayload TEST_YP2 = new YamlPayload(TEST_YAML2 + "  - " + TEST_SERVICE4 + "\n");
  // adds TEST_APP_NAME4 to, and removes TEST_APP_NAME3 from TEST_YAML2
  private final YamlPayload TEST_YP3 = new YamlPayload(TEST_YAML1 + "  - " + TEST_SERVICE4 + "\n");

  private final Application testApp1 = anApplication()
                                           .withUuid(TEST_APP_ID1)
                                           .withAppId(TEST_APP_ID1)
                                           .withName(TEST_APP_NAME1)
                                           .withDescription(TEST_APP_DESCRIPTION1)
                                           .build();

  private final Service testService1 = aService().withName(TEST_SERVICE1).build();
  private final Service testService2 = aService().withName(TEST_SERVICE2).build();
  private final Service testService3 = aService().withName(TEST_SERVICE3).build();
  private final Service testService4 = aService().withName(TEST_SERVICE4).build();

  private final List<Service> testServices1 = asList(testService1, testService2, testService3);
  private final List<Service> testServices2 = asList(testService1, testService2, testService3, testService4);

  //=============================================================================================================
  // TODO - these tests (or their equivalent) need to be rewritten given the extensive refactoring that was done
  //=============================================================================================================

  @Before
  public void init() {
    /*
    when(appService.getAppNamesByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApps);
    when(appService.getAppsByAccountId(TEST_ACCOUNT_ID)).thenReturn(testApplications);

    List<String> appNames = appService.getAppNamesByAccountId(TEST_ACCOUNT_ID);
    SetupYaml setup = new SetupYaml();
    setup.setAppNames(appNames);
    when(syr.get(TEST_ACCOUNT_ID)).thenReturn(YamlHelper.getYamlRestResponse(setup, "setup.yaml"));
    */

    when(appService.get(TEST_APP_ID1)).thenReturn(testApp1);
    when(serviceResourceService.findServicesByApp(TEST_APP_ID1)).thenReturn(testServices1);
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService, serviceResourceService);
  }

  /*
  @Test
  public void testGetYaml() {
    RestResponse<YamlPayload> actual = resources.client().target("/appYaml/" + TEST_ACCOUNT_ID + "/" +
  TEST_APP_ID1).request().get(new GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML2 + "environments: " + "\n");
  }
  */

  /*
  @Test
  public void testUpdateFromYamlNoChange() {
    RestResponse<Application> actual = resources.client().target("/appYaml/" + TEST_ACCOUNT_ID + "/" +
  TEST_APP_ID1).request().put(Entity.entity(TEST_YP, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<Application>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.GENERAL_YAML_INFO);
    assertThat(rm.getMessage()).isEqualTo("No change to the Yaml.");
  }

  @Test
  public void testUpdateFromYamlAddOnly() {
    RestResponse<Application> actual = resources.client().target("/appYaml/" + TEST_ACCOUNT_ID + "/" +
  TEST_APP_ID1).request().put(Entity.entity(TEST_YP2, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<Application>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    logger.info("************ actual = " + actual);

    Application app = actual.getResource();

    logger.info("************ app = " + app);

    AppYaml appYaml = actual.getResource();
    List<String> serviceNames = appYaml.getServiceNames();

    assertThat(serviceNames).isEqualTo(testServices2);
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteNotEnabled() {
    RestResponse<Application> actual = resources.client().target("/appYaml/" + TEST_ACCOUNT_ID + "/" +
  TEST_APP_ID1).request().put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<Application>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.NON_EMPTY_DELETIONS);
    assertThat(rm.getMessage()).isEqualTo("WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you
  want to proceed.");
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteEnabled() {
    RestResponse<Application> actual = resources.client().target("/appYaml/" + TEST_ACCOUNT_ID + "/" + TEST_APP_ID1 +
  "?deleteEnabled=true").request().put( Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new
  GenericType<RestResponse<Application>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps3);
  }
  */
}
