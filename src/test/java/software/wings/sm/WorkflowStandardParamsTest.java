
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

/**
 * @author Rishi
 *
 */
public class WorkflowStandardParamsTest extends WingsBaseTest {
  @Inject AppService appService;
  @Inject EnvironmentService environmentService;
  @Inject MembersInjector<WorkflowStandardParams> injector;

  @Test
  public void shouldGetApp() {
    Application app = Application.Builder.anApplication().withName("AppA").build();
    app = appService.save(app);

    Environment env = Environment.EnvironmentBuilder.anEnvironment().withName("DEV").build();
    env = environmentService.save(env);

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    Map<String, Object> map = std.paramMap();
    assertThat(map).isNotNull();
    assertThat(map.get(ContextElement.APP_OBJECT_NAME)).isNotNull();
    assertThat(map.get(ContextElement.APP_OBJECT_NAME)).isEqualTo(app);
    assertThat(map.get(ContextElement.ENV_OBJECT_NAME)).isNotNull();
    assertThat(map.get(ContextElement.ENV_OBJECT_NAME)).isEqualTo(env);
  }
}
