package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import io.dropwizard.testing.junit.ResourceTestRule;
import software.wings.beans.Application;
import software.wings.service.intfc.AppService;

public class AppResourceTest {
  private static final AppService appService = mock(AppService.class);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new AppResource(appService)).build();

  private final String testName = "testName-" + System.currentTimeMillis();
  private final Application testApp = getTestApplication();

  private Application getTestApplication() {
    Application app = new Application();
    app.setName(testName);
    return app;
  }

  @Before
  public void setup() {
    when(appService.findByName(testName)).thenReturn(testApp);
  }

  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService);
  }

  @Test
  public void testFindByName() {
    Application actual = resources.client().target("/apps/" + testName).request().get(Application.class);
    assertThat(actual).isEqualToComparingFieldByField(testApp);
    verify(appService).findByName(testName).equals(testApp);
  }
}
