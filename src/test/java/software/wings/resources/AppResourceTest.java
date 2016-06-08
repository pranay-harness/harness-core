package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.AppService;

import javax.ws.rs.core.GenericType;

// TODO: Auto-generated Javadoc

/**
 * The Class AppResourceTest.
 */
public class AppResourceTest {
  private static final AppService appService = mock(AppService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new AppResource(appService)).build();

  private final String TEST_UUID = "TEST-UUID-" + System.currentTimeMillis();
  private final Application testApp = anApplication().withUuid(TEST_UUID).build();

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
    when(appService.findByUuid(TEST_UUID)).thenReturn(testApp);
    RestResponse<Application> actual =
        resources.client().target("/apps/" + TEST_UUID).request().get(new GenericType<RestResponse<Application>>() {});
    assertThat(actual.getResource()).isEqualTo(testApp);
    verify(appService).findByUuid(TEST_UUID);
  }
}
