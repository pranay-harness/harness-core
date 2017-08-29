package software.wings.yaml;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.resources.ServiceYamlResource;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ResourceTestRule;

/**
 * The ServiceYamlResourceTest class.
 *
 * @author bsollish
 */
public class ServiceYamlResourceTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  // create mocks
  private static final ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new ServiceYamlResource(serviceResourceService)).build();

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
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(serviceResourceService);
  }

  @Test
  public void testGetYaml() {
    /*
    RestResponse<YamlPayload> actual = resources.client().target("/setupYaml/" + TEST_ACCOUNT_ID).request().get(new
    GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML2);
    */
  }

  @Test
  public void testUpdateFromYamlNoChange() {
    /*
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" +
    TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP, MediaType.APPLICATION_JSON), new
    GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.GENERAL_YAML_INFO);
    assertThat(rm.getMessage()).isEqualTo("No change to the Yaml.");
    */
  }

  @Test
  public void testUpdateFromYamlAddOnly() {
    /*
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" +
    TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP2, MediaType.APPLICATION_JSON), new
    GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps2);
    */
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteNotEnabled() {
    /*
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" +
    TEST_ACCOUNT_ID).request().put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new
    GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.NON_EMPTY_DELETIONS);
    assertThat(rm.getMessage()).isEqualTo("WARNING: This operation will delete objects! Pass 'deleteEnabled=true' if you
    want to proceed.");
    */
  }

  @Test
  public void testUpdateFromYamlAddAndDeleteEnabled() {
    /*
    RestResponse<SetupYaml> actual = resources.client().target("/setupYaml/" + TEST_ACCOUNT_ID +
    "?deleteEnabled=true").request().put(Entity.entity(TEST_YP3, MediaType.APPLICATION_JSON), new
    GenericType<RestResponse<SetupYaml>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(0);

    SetupYaml setupYaml = actual.getResource();
    List<String> appNames = setupYaml.getAppNames();

    assertThat(appNames).isEqualTo(testApps3);
    */
  }
}