package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.command.CleanupCommandUnit.CLEANUP_UNIT;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.InitCommandUnit.INITIALIZE_UNIT;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Builder;
import software.wings.beans.Event.Type;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EventEmitter;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionStatus;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class ActivityServiceTest extends WingsBaseTest {
  private static final Builder builder = anActivity()
                                             .withEnvironmentId(ENV_ID)
                                             .withEnvironmentName(ENV_NAME)
                                             .withEnvironmentType(PROD)
                                             .withAppId(APP_ID)
                                             .withApplicationName(APP_NAME)
                                             .withArtifactId(ARTIFACT_ID)
                                             .withArtifactName(ARTIFACT_NAME)
                                             .withCommandName(COMMAND_NAME)
                                             .withCommandType(EXEC.name())
                                             .withHostName(HOST_NAME)
                                             .withArtifactStreamId(ARTIFACT_STREAM_NAME)
                                             .withServiceName(SERVICE_NAME)
                                             .withServiceId(SERVICE_ID)
                                             .withServiceTemplateName(TEMPLATE_NAME)
                                             .withServiceTemplateId(TEMPLATE_ID)
                                             .withStatus(ExecutionStatus.RUNNING)
                                             .withWorkflowExecutionId("WORKFLOW_ID")
                                             .withWorkflowExecutionName("Workflow 1")
                                             .withWorkflowType(WorkflowType.SIMPLE)
                                             .withStateExecutionInstanceId("STATE_ID")
                                             .withStateExecutionInstanceName("STATE")
                                             .withServiceInstanceId(SERVICE_INSTANCE_ID);

  @Inject private WingsPersistence wingsPersistence;

  @Mock private ServiceResourceService serviceResourceService;

  @Mock private ServiceInstanceService serviceInstanceService;

  @Mock private EventEmitter eventEmitter;

  @Inject @InjectMocks private ActivityService activityService;

  /**
   * Should list activities.
   */
  @Test
  public void shouldListActivities() {
    Activity activity = builder.but().build();
    wingsPersistence.save(activity);
    assertThat(activityService.list(new PageRequest<>())).hasSize(1).containsExactly(activity);
  }

  /**
   * Should get activity.
   */
  @Test
  public void shouldGetActivity() {
    Activity activity = builder.but().build();
    wingsPersistence.save(activity);
    assertThat(activityService.get(activity.getUuid(), activity.getAppId())).isEqualTo(activity);
  }

  /**
   * Should save activity.
   */
  @Test
  public void shouldSaveActivity() {
    Activity activity = builder.but().build();
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
    Activity activity = builder.but().withCommandNameVersionMap(ImmutableMap.of(COMMAND_NAME, 1)).build();
    String activityId = wingsPersistence.save(activity);
    Command command =
        aCommand()
            .withName(COMMAND_NAME)
            .addCommandUnits(
                anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("./bin/start.sh").build())
            .build();
    when(serviceResourceService.getCommandByNameAndVersion(APP_ID, SERVICE_ID, COMMAND_NAME, 1))
        .thenReturn(aServiceCommand().withCommand(command).build());
    List<CommandUnit> commandUnits = activityService.getCommandUnits(APP_ID, activityId);
    assertThat(commandUnits)
        .hasSize(3)
        .extracting(CommandUnit::getCommandUnitType, CommandUnit::getName)
        .contains(tuple(EXEC, INITIALIZE_UNIT), tuple(EXEC, COMMAND_UNIT_NAME), tuple(EXEC, CLEANUP_UNIT));
  }

  /**
   * Shouldget last activity for service.
   */
  @Test
  public void shouldGetLastActivityForService() {
    Activity activity = builder.but().build();
    wingsPersistence.save(activity);
    Activity activityForService = activityService.getLastActivityForService(APP_ID, SERVICE_ID);
    assertThat(activityForService).isEqualTo(activity);
  }

  /**
   * Shouldget last production activity for service.
   */
  @Test
  public void shouldGetLastProductionActivityForService() {
    Activity activity = builder.but().build();
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
    Activity activity = builder.but().build();
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

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);

    activity.setStatus(ExecutionStatus.SUCCESS);
    verify(serviceInstanceService, times(2)).updateActivity(activity);
    verify(eventEmitter)
        .send(Channel.ACTIVITIES,
            anEvent()
                .withUuid(activity.getUuid())
                .withEnvId(activity.getEnvironmentId())
                .withAppId(activity.getAppId())
                .withType(Type.UPDATE)
                .build());
  }
}
