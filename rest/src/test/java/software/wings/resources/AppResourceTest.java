package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Setup.SetupStatus.COMPLETE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.RestRequest;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.AppService;
import software.wings.utils.ResourceTestRule;
import software.wings.yaml.YamlPayload;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * The Class AppResourceTest.
 */
public class AppResourceTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final AppService appService = mock(AppService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new AppResource(appService)).build();
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_UUID = "TEST-UUID-" + TIME_IN_MS;
  private final String TEST_UUID2 = "TEST-UUID2-" + TIME_IN_MS + 10;
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID-" + TIME_IN_MS;
  private final String TEST_NAME = "TestApp_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION = "stuff";
  private final String TEST_YAML =
      "--- # app.yaml for new Application\nname: " + TEST_NAME + "\ndescription: " + TEST_DESCRIPTION;

  private final Application testApp = anApplication().withUuid(TEST_UUID).build();
  private final Application testApp2 = anApplication()
                                           .withUuid(TEST_UUID2)
                                           .withAccountId(TEST_ACCOUNT_ID)
                                           .withAppId(TEST_UUID2)
                                           .withName(TEST_NAME)
                                           .withDescription(TEST_DESCRIPTION)
                                           .build();

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService);
  }

  /**
   * Test find by name.
   */
  @Test
  public void testFindByName() {
    when(appService.get(TEST_UUID, COMPLETE, true, 30)).thenReturn(testApp);
    RestResponse<Application> actual =
        resources.client().target("/apps/" + TEST_UUID).request().get(new GenericType<RestResponse<Application>>() {});
    assertThat(actual.getResource()).isEqualTo(testApp);
    verify(appService).get(TEST_UUID, COMPLETE, true, 30);
  }

  @Test
  public void testGetYaml() {
    when(appService.get(TEST_UUID2, COMPLETE, true, 30)).thenReturn(testApp2);
    RestResponse<YamlPayload> actual = resources.client()
                                           .target("/apps/yaml/" + TEST_UUID2)
                                           .request()
                                           .get(new GenericType<RestResponse<YamlPayload>>() {});

    YamlPayload yp = actual.getResource();
    String yaml = yp.getYaml();

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    try {
      Application app = mapper.readValue(yaml, Application.class);

      assertThat(app.getName()).isEqualTo(TEST_NAME);
      assertThat(app.getDescription()).isEqualTo(TEST_DESCRIPTION);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  // This (POST) Unit Test doesn't really do anyting (useful) – moved to: YamlPayloadIntegrationTest
  @Test
  public void testSaveFromYaml() {
    YamlPayload yp = new YamlPayload(TEST_YAML);

    // when we save ANY instance of Application, we want to return testApp2
    when(appService.save(Mockito.any(Application.class))).thenReturn(testApp2);
    RestResponse<Application> actual =
        resources.client()
            .target("/apps/yaml?accountId=" + TEST_ACCOUNT_ID)
            .request()
            .post(Entity.entity(yp, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Application>>() {});
    Application app = actual.getResource();

    assertThat(app.getName()).isEqualTo(TEST_NAME);
    assertThat(app.getDescription()).isEqualTo(TEST_DESCRIPTION);
  }

  // A (PUT) Unit Test wonn't really do anyting (useful) – moved to: YamlPayloadIntegrationTest
  @Test
  public void testUpdateFromYaml() {}
}
