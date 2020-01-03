package software.wings.resources;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.service.intfc.AppService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

/**
 * The Class AppResourceTest.
 */
@Slf4j
public class AppResourceTest extends CategoryTest {
  private static final AppService appService = mock(AppService.class);
  private static final LimitCheckerFactory limitCheckerFactory = mock(LimitCheckerFactory.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new AppResource(appService, limitCheckerFactory)).build();
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_UUID = "TEST-UUID-" + TIME_IN_MS;
  private final String TEST_UUID2 = "TEST-UUID2-" + TIME_IN_MS + 10;
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID-" + TIME_IN_MS;
  private final String TEST_NAME = "TestApp_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION = "stuff";
  private final String TEST_YAML =
      "--- # app.yaml for new Application\nname: " + TEST_NAME + "\ndescription: " + TEST_DESCRIPTION;

  private final Application testApp = anApplication().uuid(TEST_UUID).build();
  private final Application testApp2 = anApplication()
                                           .uuid(TEST_UUID2)
                                           .accountId(TEST_ACCOUNT_ID)
                                           .appId(TEST_UUID2)
                                           .name(TEST_NAME)
                                           .description(TEST_DESCRIPTION)
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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testFindByName() {
    when(appService.get(TEST_UUID, true)).thenReturn(testApp);
    RestResponse<Application> actual =
        resources.client().target("/apps/" + TEST_UUID).request().get(new GenericType<RestResponse<Application>>() {});
    assertThat(actual.getResource()).isEqualTo(testApp);
    verify(appService).get(TEST_UUID, true);
  }
}
