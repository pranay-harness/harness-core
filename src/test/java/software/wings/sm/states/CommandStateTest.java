package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.api.SimpleWorkflowParam.Builder.aSimpleWorkflowParam;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Release.Builder.aRelease;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandUnit.ExecutionResult.ExecutionResultData.Builder.anExecutionResultData;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RELEASE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.ApplicationHost;
import software.wings.beans.Artifact;
import software.wings.beans.Host;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.command.Command;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

/**
 * Created by peeyushaggarwal on 6/10/16.
 */
public class CommandStateTest extends WingsBaseTest {
  /**
   * The constant RUNTIME_PATH.
   */
  public static final String RUNTIME_PATH = "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/runtime";
  /**
   * The constant BACKUP_PATH.
   */
  public static final String BACKUP_PATH =
      "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/backup/${timestampId}";
  /**
   * The constant STAGING_PATH.
   */
  public static final String STAGING_PATH =
      "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/staging/${timestampId}";
  private static final Command COMMAND = aCommand().build();
  private static final Service SERVICE = aService().withUuid(SERVICE_ID).build();
  private static final ServiceInstance SERVICE_INSTANCE =
      aServiceInstance()
          .withUuid(SERVICE_INSTANCE_ID)
          .withAppId(APP_ID)
          .withEnvId(ENV_ID)
          .withServiceTemplate(aServiceTemplate().withUuid(TEMPLATE_ID).withService(SERVICE).build())
          .withHost(ApplicationHost.Builder.anApplicationHost()
                        .withAppId(APP_ID)
                        .withEnvId(ENV_ID)
                        .withUuid(HOST_ID)
                        .withHost(Host.Builder.aHost().withHostName(HOST_NAME).build())
                        .build())
          .build();

  private static final Activity ACTIVITY_WITH_ID =
      anActivity()
          .withUuid(ACTIVITY_ID)
          .withAppId(APP_ID)
          .withApplicationName(APP_NAME)
          .withEnvironmentId(SERVICE_INSTANCE.getEnvId())
          .withServiceTemplateId(SERVICE_INSTANCE.getServiceTemplate().getUuid())
          .withServiceTemplateName(SERVICE_INSTANCE.getServiceTemplate().getName())
          .withServiceId(SERVICE_INSTANCE.getServiceTemplate().getService().getUuid())
          .withServiceName(SERVICE_INSTANCE.getServiceTemplate().getService().getName())
          .withCommandName(COMMAND.getName())
          .withCommandType(COMMAND.getCommandUnitType().name())
          .withHostName(SERVICE_INSTANCE.getHost().getHostName())
          .withServiceInstanceId(SERVICE_INSTANCE_ID)
          .build();
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  private static final SimpleWorkflowParam SIMPLE_WORKFLOW_PARAM = aSimpleWorkflowParam().build();

  @Inject private ExecutorService executorService;

  @Mock private AppService appService;
  @Mock private ExecutionContextImpl context;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceCommandExecutorService serviceCommandExecutorService;
  @Mock private ActivityService activityService;
  @Mock private SettingsService settingsService;
  @Mock private EnvironmentService environmentService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private ReleaseService releaseService;
  @InjectMocks private CommandState commandState = new CommandState("start1", "START");

  /**
   * Sets up mocks.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUpMocks() throws Exception {
    when(appService.get(APP_ID)).thenReturn(anApplication().withUuid(APP_ID).withName(APP_NAME).build());
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build());
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, "START")).thenReturn(COMMAND);
    when(serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID)).thenReturn(SERVICE_INSTANCE);
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);

    when(activityService.get(ACTIVITY_ID, APP_ID)).thenReturn(ACTIVITY_WITH_ID);
    when(serviceCommandExecutorService.execute(SERVICE_INSTANCE, COMMAND,
             aCommandExecutionContext()
                 .withAppId(APP_ID)
                 .withBackupPath(BACKUP_PATH)
                 .withRuntimePath(RUNTIME_PATH)
                 .withStagingPath(STAGING_PATH)
                 .withExecutionCredential(null)
                 .withActivityId(ACTIVITY_ID)
                 .build()))
        .thenReturn(SUCCESS);
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.RUNTIME_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(RUNTIME_PATH).build()).build());
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.BACKUP_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(BACKUP_PATH).build()).build());
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.STAGING_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(STAGING_PATH).build()).build());

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(asList(SIMPLE_WORKFLOW_PARAM));

    when(context.getContextElement(ContextElementType.SERVICE))
        .thenReturn(aServiceElement().withUuid(SERVICE_ID).build());
    when(context.getContextElement(ContextElementType.INSTANCE))
        .thenReturn(anInstanceElement().withUuid(SERVICE_INSTANCE_ID).build());
    when(context.renderExpression(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
    commandState.setExecutorService(executorService);
  }

  /**
   * Execute.
   *
   * @throws Exception the exception
   */
  @Test
  public void execute() throws Exception {
    when(serviceCommandExecutorService.execute(eq(SERVICE_INSTANCE), eq(COMMAND), any())).thenReturn(SUCCESS);
    commandState.execute(context);
    commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, anExecutionResultData().withResult(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, "START");
    verify(serviceInstanceService, times(2)).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);

    verify(activityService).save(any(Activity.class));
    verify(serviceInstanceService, times(2)).updateActivity(any(Activity.class));
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(activityService).get(ACTIVITY_ID, APP_ID);

    verify(serviceCommandExecutorService)
        .execute(SERVICE_INSTANCE, COMMAND,
            aCommandExecutionContext()
                .withAppId(APP_ID)
                .withBackupPath(BACKUP_PATH)
                .withRuntimePath(RUNTIME_PATH)
                .withStagingPath(STAGING_PATH)
                .withExecutionCredential(null)
                .withActivityId(ACTIVITY_ID)
                .withServiceInstance(SERVICE_INSTANCE)
                .build());

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(2)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(3)).getWorkflowExecutionId();
    verify(context, times(1)).getWorkflowExecutionName();
    verify(context, times(1)).getWorkflowType();
    verify(context, times(1)).getStateExecutionInstanceId();
    verify(context, times(1)).getStateExecutionInstanceName();

    verify(context, times(4)).renderExpression(anyString());

    verify(settingsService, times(3)).getByName(eq(APP_ID), eq(ENV_ID), anyString());

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));

    verify(waitNotifyEngine).notify(ACTIVITY_ID, anExecutionResultData().withResult(SUCCESS).build());

    verifyNoMoreInteractions(context, serviceResourceService, serviceInstanceService, serviceCommandExecutorService,
        activityService, settingsService, workflowExecutionService, releaseService);
  }

  /**
   * Execute with artifact.
   *
   * @throws Exception the exception
   */
  @Test
  public void executeWithArtifact() throws Exception {
    Artifact artifact = anArtifact()
                            .withUuid(ARTIFACT_ID)
                            .withRelease(aRelease().withUuid(RELEASE_ID).build())
                            .withServices(asList(SERVICE))
                            .build();

    Command command =
        aCommand()
            .addCommandUnits(
                ScpCommandUnit.Builder.aScpCommandUnit().withFileCategory(ScpFileCategory.ARTIFACTS).build())
            .build();

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    on(workflowStandardParams).set("artifacts", asList(artifact));
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, "START")).thenReturn(command);

    when(serviceCommandExecutorService.execute(SERVICE_INSTANCE, command,
             aCommandExecutionContext()
                 .withAppId(APP_ID)
                 .withBackupPath(BACKUP_PATH)
                 .withRuntimePath(RUNTIME_PATH)
                 .withStagingPath(STAGING_PATH)
                 .withExecutionCredential(null)
                 .withActivityId(ACTIVITY_ID)
                 .withArtifact(artifact)
                 .build()))
        .thenReturn(SUCCESS);

    commandState.execute(context);
    commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, anExecutionResultData().withResult(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, "START");
    verify(serviceInstanceService, times(2)).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(activityService).save(any(Activity.class));
    verify(serviceInstanceService, times(2)).updateActivity(any(Activity.class));

    verify(serviceCommandExecutorService)
        .execute(SERVICE_INSTANCE, command,
            aCommandExecutionContext()
                .withAppId(APP_ID)
                .withBackupPath(BACKUP_PATH)
                .withRuntimePath(RUNTIME_PATH)
                .withStagingPath(STAGING_PATH)
                .withExecutionCredential(null)
                .withActivityId(ACTIVITY_ID)
                .withArtifact(artifact)
                .withServiceInstance(SERVICE_INSTANCE)
                .build());

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(2)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(3)).getWorkflowExecutionId();
    verify(context, times(1)).getWorkflowType();
    verify(context, times(4)).renderExpression(anyString());
    verify(context, times(1)).getWorkflowExecutionName();
    verify(context, times(1)).getStateExecutionInstanceId();
    verify(context, times(1)).getStateExecutionInstanceName();

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(activityService).get(ACTIVITY_ID, APP_ID);
    verify(settingsService, times(3)).getByName(eq(APP_ID), eq(ENV_ID), anyString());

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));

    verifyNoMoreInteractions(context, serviceResourceService, serviceInstanceService, activityService,
        serviceCommandExecutorService, settingsService, workflowExecutionService, releaseService);
  }
}
