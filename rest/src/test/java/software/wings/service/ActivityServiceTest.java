package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.command.CleanupSshCommandUnit.CLEANUP_UNIT;
import static software.wings.beans.command.CommandUnitDetails.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.InitSshCommandUnit.INITIALIZE_UNIT;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.beans.SearchFilter.Operator;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Event.Type;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ExecutionStatus;

import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class ActivityServiceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private LogService logService;

  @Mock private EventEmitter eventEmitter;

  @Inject @InjectMocks private ActivityService activityService;

  /**
   * Should list activities.
   */
  @Test
  public void shouldListActivities() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .build();
    activity.setAppId(APP_ID);
    wingsPersistence.save(activity);
    assertThat(activityService.list(aPageRequest().addFilter(Activity.APP_ID_KEY, Operator.EQ, APP_ID).build()))
        .hasSize(1)
        .containsExactly(activity);
  }

  /**
   * Should get activity.
   */
  @Test
  public void shouldGetActivity() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .build();
    activity.setAppId(APP_ID);
    wingsPersistence.save(activity);
    assertThat(activityService.get(activity.getUuid(), activity.getAppId())).isEqualTo(activity);
  }

  /**
   * Should save activity.
   */
  @Test
  public void shouldSaveActivity() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandUnits(Collections.emptyList())
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .build();
    activity.setAppId(APP_ID);
    activityService.save(activity);
    assertThat(wingsPersistence.get(Activity.class, activity.getAppId(), activity.getUuid())).isEqualTo(activity);
    verify(serviceInstanceService).updateActivity(activity);
    verify(eventEmitter)
        .send(Channel.ACTIVITIES,
            anEvent()
                .withUuid(activity.getUuid())
                .withEnvId(activity.getEnvironmentId())
                .withAppId(activity.getAppId())
                .withType(Type.CREATE)
                .build());
  }

  /**
   * Should get activity command units.
   */
  @Test
  public void shouldGetActivityCommandUnits() {
    List<CommandUnit> commandUnitList = asList(new InitSshCommandUnit(),
        anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("./bin/start.sh").build(),
        new CleanupSshCommandUnit());
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandName(COMMAND_NAME)
                            .commandType(CommandUnitType.COMMAND.name())
                            .commandUnits(commandUnitList)
                            .build();
    activity.setAppId(APP_ID);

    String activityId = wingsPersistence.save(activity);
    List<CommandUnitDetails> commandUnits = activityService.getCommandUnits(APP_ID, activityId);
    assertThat(commandUnits)
        .hasSize(3)
        .extracting(CommandUnitDetails::getCommandUnitType, CommandUnitDetails::getName)
        .contains(tuple(COMMAND, INITIALIZE_UNIT), tuple(COMMAND, COMMAND_UNIT_NAME), tuple(COMMAND, CLEANUP_UNIT));
  }

  /**
   * Shouldget last activity for service.
   */
  @Test
  public void shouldGetLastActivityForService() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .build();
    activity.setAppId(APP_ID);
    wingsPersistence.save(activity);
    Activity activityForService = activityService.getLastActivityForService(APP_ID, SERVICE_ID);
    assertThat(activityForService).isEqualTo(activity);
  }

  /**
   * Shouldget last production activity for service.
   */
  @Test
  public void shouldGetLastProductionActivityForService() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .build();
    activity.setAppId(APP_ID);
    activity.setEnvironmentType(PROD);
    wingsPersistence.save(activity);
    Activity lastProductionActivityForService = activityService.getLastProductionActivityForService(APP_ID, SERVICE_ID);
    assertThat(lastProductionActivityForService).isEqualTo(activity);
  }

  /**
   * Should update activity status.
   */
  @Test
  public void shouldUpdateActivityStatus() {
    Activity activity = Activity.builder()
                            .environmentId(ENV_ID)
                            .environmentName(ENV_NAME)
                            .environmentType(PROD)
                            .applicationName(APP_NAME)
                            .artifactId(ARTIFACT_ID)
                            .artifactName(ARTIFACT_NAME)
                            .commandName(COMMAND_NAME)
                            .commandType(EXEC.name())
                            .hostName(HOST_NAME)
                            .artifactStreamId(ARTIFACT_STREAM_NAME)
                            .serviceName(SERVICE_NAME)
                            .serviceId(SERVICE_ID)
                            .serviceTemplateName(TEMPLATE_NAME)
                            .serviceTemplateId(TEMPLATE_ID)
                            .status(ExecutionStatus.RUNNING)
                            .workflowExecutionId("WORKFLOW_ID")
                            .workflowExecutionName("Workflow 1")
                            .workflowType(WorkflowType.SIMPLE)
                            .stateExecutionInstanceId("STATE_ID")
                            .stateExecutionInstanceName("STATE")
                            .workflowId(WORKFLOW_ID)
                            .commandUnits(Collections.emptyList())
                            .serviceVariables(Maps.newHashMap())
                            .serviceInstanceId(SERVICE_INSTANCE_ID)
                            .build();
    activity.setAppId(APP_ID);
    activity.setUuid(ACTIVITY_ID);
    activityService.save(activity);
    assertThat(wingsPersistence.get(Activity.class, activity.getAppId(), activity.getUuid())).isEqualTo(activity);
    verify(eventEmitter)
        .send(Channel.ACTIVITIES,
            anEvent()
                .withUuid(activity.getUuid())
                .withEnvId(activity.getEnvironmentId())
                .withAppId(activity.getAppId())
                .withType(Type.CREATE)
                .build());

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);

    activity.setStatus(ExecutionStatus.SUCCESS);
    verify(serviceInstanceService, times(2)).updateActivity(anyObject());
    verify(eventEmitter)
        .send(Channel.ACTIVITIES,
            anEvent()
                .withUuid(activity.getUuid())
                .withEnvId(activity.getEnvironmentId())
                .withAppId(activity.getAppId())
                .withType(Type.UPDATE)
                .build());
  }

  @Test
  public void shouldPruneDescendingObjects() {
    activityService.pruneDescendingEntities(APP_ID, ACTIVITY_ID);
    InOrder inOrder = inOrder(logService);
    inOrder.verify(logService).pruneByActivity(APP_ID, ACTIVITY_ID);
  }
}
