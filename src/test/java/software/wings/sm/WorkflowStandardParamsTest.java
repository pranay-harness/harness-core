/**
 *
 */

package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.MembersInjector;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;

import java.util.Map;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class WorkflowStandardParamsTest.
 *
 * @author Rishi
 */
public class WorkflowStandardParamsTest extends WingsBaseTest {
  /**
   * The App service.
   */
  @Inject AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;
  /**
   * The Injector.
   */
  @Inject MembersInjector<WorkflowStandardParams> injector;

  /**
   * Should get app.
   */
  @Test
  public void shouldGetApp() {
    Application app = Application.Builder.anApplication().withName("AppA").build();
    app = appService.save(app);

    Environment env = Environment.EnvironmentBuilder.anEnvironment().withAppId(app.getUuid()).withName("DEV").build();
    env = environmentService.save(env);
    app = appService.get(app.getUuid());

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    Map<String, Object> map = std.paramMap();
    assertThat(map).isNotNull().containsEntry(ContextElement.APP, app).containsEntry(ContextElement.ENV, env);
  }
}
