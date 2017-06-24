/**
 *
 */

package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.Lists;
import com.google.inject.MembersInjector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Map;
import javax.inject.Inject;

/**
 * The Class WorkflowStandardParamsTest.
 *
 * @author Rishi
 */
public class WorkflowStandardParamsTest extends WingsBaseTest {
  /**
   * The App service.
   */
  @Inject @InjectMocks AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;
  /**
   * The Injector.
   */
  @Inject MembersInjector<WorkflowStandardParams> injector;

  @Mock SettingsService settingsService;
  @Mock private JobScheduler jobScheduler;
  @Mock private ExecutionContext context;

  @Before
  public void setup() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.APP_DYNAMICS.name()))
        .thenReturn(Lists.newArrayList(aSettingAttribute().withUuid("id").build()));
    on(appService).set("settingsService", settingsService);
  }

  /**
   * Should get app.
   */
  @Test
  public void shouldGetApp() {
    Application app = Application.Builder.anApplication().withName("AppA").build();
    app = appService.save(app);

    Environment env = Builder.anEnvironment().withAppId(app.getUuid()).withName("DEV").build();
    env = environmentService.save(env);
    app = appService.get(app.getUuid());

    WorkflowStandardParams std = new WorkflowStandardParams();
    injector.injectMembers(std);
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceElement);

    Map<String, Object> map = std.paramMap(context);
    assertThat(map).isNotNull().containsEntry(ContextElement.APP, app).containsEntry(ContextElement.ENV, env);
  }
}
