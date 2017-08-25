package software.wings.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Service.Builder.aService;

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
import software.wings.beans.Service;
import software.wings.resources.AppYamlResource;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ResourceTestRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * The AppYamlResourceTest class.
 *
 * @author bsollish
 */
public class AppYamlResourceTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  // create mocks
  private static final AppService appService = mock(AppService.class);
  private static final ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new AppYamlResource(appService, serviceResourceService)).build();

  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID" + TIME_IN_MS;

  private final String TEST_APP_ID1 = "TEST-APP-ID" + TIME_IN_MS;
  private final String TEST_APP_NAME1 = "TEST-APP-NAME" + TIME_IN_MS;
  private final String TEST_APP_DESCRIPTION1 = "TEST-APP-DESCRIPTION" + TIME_IN_MS;

  private final String TEST_SERVICE1 = "TEST-SERVICE-" + TIME_IN_MS;
  private final String TEST_SERVICE2 = "TEST-SERVICE-" + TIME_IN_MS + 10;
  private final String TEST_SERVICE3 = "TEST-SERVICE-" + TIME_IN_MS + 20;
  private final String TEST_SERVICE4 = "TEST-SERVICE-" + TIME_IN_MS + 30;

  private final String TEST_YAML1 = "description: " + TEST_APP_DESCRIPTION1 + "\nname: " + TEST_APP_NAME1
      + "\nservices:\n- " + TEST_SERVICE1 + "\n- " + TEST_SERVICE2 + "\n";
  private final String TEST_YAML2 = TEST_YAML1 + "- " + TEST_SERVICE3 + "\n";
  private final YamlPayload TEST_YP = new YamlPayload(TEST_YAML2);

  private final Application testApp1 = anApplication()
                                           .withUuid(TEST_APP_ID1)
                                           .withAppId(TEST_APP_ID1)
                                           .withName(TEST_APP_NAME1)
                                           .withDescription(TEST_APP_DESCRIPTION1)
                                           .build();

  private final Service testService1 = aService().withName(TEST_SERVICE1).build();
  private final Service testService2 = aService().withName(TEST_SERVICE2).build();
  private final Service testService3 = aService().withName(TEST_SERVICE3).build();

  private final List<Service> testServices1 =
      new ArrayList<Service>(Arrays.asList(testService1, testService2, testService3));

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

  @Test
  public void testGetYaml() {
    RestResponse<YamlPayload> actual = resources.client()
                                           .target("/appYaml/" + TEST_ACCOUNT_ID + "/" + TEST_APP_ID1)
                                           .request()
                                           .get(new GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    assertThat(yaml).isEqualTo(TEST_YAML2);
  }

  @Test
  public void testUpdateFromYamlNoChange() {
    RestResponse<Application> actual =
        resources.client()
            .target("/appYaml/" + TEST_ACCOUNT_ID + "/" + TEST_APP_ID1)
            .request()
            .put(Entity.entity(TEST_YP, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Application>>() {});

    assertThat(actual.getResponseMessages().size()).isEqualTo(1);

    ResponseMessage rm = actual.getResponseMessages().get(0);

    assertThat(rm.getCode()).isEqualTo(ErrorCode.GENERAL_YAML_INFO);
    assertThat(rm.getMessage()).isEqualTo("No change to the Yaml.");
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
