package software.wings.sm.states.k8s;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StepType.K8S_SCALE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.k8s.K8sScale.K8S_SCALE_COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EventEmitter;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;

public class K8sStateHelperTest extends WingsBaseTest {
  @Mock private ActivityService activityService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private EventEmitter eventEmitter;

  @Inject @InjectMocks private K8sStateHelper k8sStateHelper;

  private ExecutionContextImpl context;
  private WorkflowStandardParams workflowStandardParams =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .displayName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addStateExecutionData(K8sStateExecutionData.builder().build())
          .build();

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(appService.getApplicationWithDefaults(APP_ID))
        .thenReturn(anApplication().appId(APP_ID).name(APP_NAME).uuid(APP_ID).build());
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment()
                        .appId(APP_ID)
                        .environmentType(EnvironmentType.PROD)
                        .uuid(ENV_ID)
                        .build());
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateK8sActivity() {
    List<CommandUnit> commandUnits = new ArrayList<>();
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Init));
    commandUnits.add(new K8sDummyCommandUnit(K8sDummyCommandUnit.Scale));

    k8sStateHelper.createK8sActivity(context, K8S_SCALE_COMMAND_NAME, K8S_SCALE.name(), activityService, commandUnits);

    ArgumentCaptor<Activity> activityArgumentCaptor = ArgumentCaptor.forClass(Activity.class);
    verify(activityService, times(1)).save(activityArgumentCaptor.capture());
    Activity activity = activityArgumentCaptor.getValue();
    assertThat(activity.getAppId()).isEqualTo(APP_ID);
    assertThat(activity.getEnvironmentId()).isEqualTo(ENV_ID);
    assertThat(activity.getCommandName()).isEqualTo(K8S_SCALE_COMMAND_NAME);
    assertThat(activity.getCommandType()).isEqualTo(K8S_SCALE.name());
    assertThat(activity.getCommandUnits()).isEqualTo(commandUnits);
  }
}
