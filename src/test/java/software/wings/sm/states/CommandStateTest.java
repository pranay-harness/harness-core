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
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.CopyArtifactCommandUnit.Builder.aCopyArtifactCommandUnit;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RELEASE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.Artifact;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

/**
 * Created by peeyushaggarwal on 6/10/16.
 */
public class CommandStateTest extends WingsBaseTest {
  public static final String RUNTIME_PATH = "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/runtime";
  public static final String BACKUP_PATH =
      "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/backup/${timestampId}";
  public static final String STAGING_PATH =
      "$HOME/${app.name}/${service.name}/${serviceTemplate.name}/staging/${timestampId}";
  private static final Command COMMAND = aCommand().build();
  private static final Service SERVICE = aService().withUuid(SERVICE_ID).build();
  private static final ServiceInstance SERVICE_INSTANCE =
      aServiceInstance()
          .withUuid(SERVICE_INSTANCE_ID)
          .withServiceTemplate(aServiceTemplate().withUuid(TEMPLATE_ID).withService(SERVICE).build())
          .withHost(aHost().withHostName(HOST_NAME).build())
          .build();
  private static final Activity ACTIVITY =
      anActivity()
          .withAppId(SERVICE_INSTANCE.getAppId())
          .withEnvironmentId(SERVICE_INSTANCE.getEnvId())
          .withServiceTemplateId(SERVICE_INSTANCE.getServiceTemplate().getUuid())
          .withServiceTemplateName(SERVICE_INSTANCE.getServiceTemplate().getName())
          .withServiceId(SERVICE_INSTANCE.getServiceTemplate().getService().getUuid())
          .withServiceName(SERVICE_INSTANCE.getServiceTemplate().getService().getName())
          .withCommandName(COMMAND.getName())
          .withCommandType(COMMAND.getCommandUnitType().name())
          .withHostName(SERVICE_INSTANCE.getHost().getHostName())
          .build();
  private static final Activity ACTIVITY_WITH_ID =
      anActivity()
          .withUuid(ACTIVITY_ID)
          .withAppId(SERVICE_INSTANCE.getAppId())
          .withEnvironmentId(SERVICE_INSTANCE.getEnvId())
          .withServiceTemplateId(SERVICE_INSTANCE.getServiceTemplate().getUuid())
          .withServiceTemplateName(SERVICE_INSTANCE.getServiceTemplate().getName())
          .withServiceId(SERVICE_INSTANCE.getServiceTemplate().getService().getUuid())
          .withServiceName(SERVICE_INSTANCE.getServiceTemplate().getService().getName())
          .withCommandName(COMMAND.getName())
          .withCommandType(COMMAND.getCommandUnitType().name())
          .withHostName(SERVICE_INSTANCE.getHost().getHostName())
          .build();
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  @Mock private ExecutionContextImpl context;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceCommandExecutorService serviceCommandExecutorService;
  @Mock private ActivityService activityService;
  @Mock private SettingsService settingsService;
  @InjectMocks private CommandState commandState = new CommandState("start1", "START");

  @Before
  public void setUpMocks() throws Exception {
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, "START")).thenReturn(COMMAND);
    when(serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID)).thenReturn(SERVICE_INSTANCE);
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);

    when(serviceCommandExecutorService.execute(SERVICE_INSTANCE, COMMAND,
             aCommandExecutionContext()
                 .withAppId(APP_ID)
                 .withBackupPath(BACKUP_PATH)
                 .withRuntimePath(RUNTIME_PATH)
                 .withStagingPath(STAGING_PATH)
                 .withExecutionCredential(null)
                 .withActivityId(ACTIVITY_ID)
                 .build()))
        .thenReturn(ExecutionResult.SUCCESS);
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.RUNTIME_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(RUNTIME_PATH).build()).build());
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.BACKUP_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(BACKUP_PATH).build()).build());
    when(settingsService.getByName(APP_ID, ENV_ID, CommandState.STAGING_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(STAGING_PATH).build()).build());

    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);

    when(context.getContextElement(ContextElementType.SERVICE))
        .thenReturn(aServiceElement().withUuid(SERVICE_ID).build());
    when(context.getContextElement(ContextElementType.INSTANCE))
        .thenReturn(anInstanceElement().withUuid(SERVICE_INSTANCE_ID).build());
    when(context.renderExpression(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
  }

  @Test
  public void execute() throws Exception {
    commandState.execute(context);

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, "START");
    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(activityService).save(any(Activity.class));

    verify(serviceCommandExecutorService)
        .execute(SERVICE_INSTANCE, COMMAND,
            aCommandExecutionContext()
                .withAppId(APP_ID)
                .withBackupPath(BACKUP_PATH)
                .withRuntimePath(RUNTIME_PATH)
                .withStagingPath(STAGING_PATH)
                .withExecutionCredential(null)
                .withActivityId(ACTIVITY_ID)
                .build());

    verify(context).getContextElement(ContextElementType.STANDARD);
    verify(context).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(4)).renderExpression(anyString());
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, Status.COMPLETED);
    verify(settingsService, times(3)).getByName(eq(APP_ID), eq(ENV_ID), anyString());
    verifyNoMoreInteractions(context, serviceResourceService, serviceInstanceService, activityService,
        serviceCommandExecutorService, settingsService);
  }

  @Test
  public void executeWithArtifact() throws Exception {
    Artifact artifact = anArtifact()
                            .withUuid(ARTIFACT_ID)
                            .withArtifactFiles(asList(anArtifactFile().withServices(asList(SERVICE)).build()))
                            .withRelease(aRelease().withUuid(RELEASE_ID).build())
                            .build();

    Command command = aCommand().addCommandUnits(aCopyArtifactCommandUnit().build()).build();

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
        .thenReturn(ExecutionResult.SUCCESS);

    commandState.execute(context);

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, "START");
    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(activityService).save(any(Activity.class));

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
                .build());

    verify(context).getContextElement(ContextElementType.STANDARD);
    verify(context).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(4)).renderExpression(anyString());
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, Status.COMPLETED);
    verify(settingsService, times(3)).getByName(eq(APP_ID), eq(ENV_ID), anyString());
    verifyNoMoreInteractions(context, serviceResourceService, serviceInstanceService, activityService,
        serviceCommandExecutorService, settingsService);
  }
}
