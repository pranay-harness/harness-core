package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.api.SimpleWorkflowParam.Builder.aSimpleWorkflowParam;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.Constants.WINDOWS_RUNTIME_PATH;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.JENKINS_URL;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.Variable;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
  /**
   * The constant WINDOWS_RUNTIME_PATH.
   */
  public static final String WINDOWS_RUNTIME_PATH_TEST =
      "%USERPROFILE%/${app.name}/${service.name}/${env.name}/runtime/test";

  private static final Command COMMAND =
      aCommand()
          .addCommandUnits(aCommand()
                               .withReferenceId("Start")
                               .addCommandUnits(anExecCommandUnit().withCommandString("${var2}").build())
                               .build())
          .build();
  private static final Service SERVICE = Service.builder().uuid(SERVICE_ID).build();
  private static final ServiceTemplate SERVICE_TEMPLATE =
      aServiceTemplate().withUuid(TEMPLATE_ID).withServiceId(SERVICE.getUuid()).build();
  private static final Host HOST =
      aHost().withUuid(HOST_ID).withHostName(HOST_NAME).withHostConnAttr("1").withBastionConnAttr("1").build();
  private static final ServiceInstance SERVICE_INSTANCE = aServiceInstance()
                                                              .withUuid(SERVICE_INSTANCE_ID)
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withServiceTemplate(SERVICE_TEMPLATE)
                                                              .withHost(HOST)
                                                              .build();
  private static final Activity ACTIVITY_WITH_ID = Activity.builder()
                                                       .applicationName(APP_NAME)
                                                       .environmentId(SERVICE_INSTANCE.getEnvId())
                                                       .serviceTemplateId(SERVICE_INSTANCE.getServiceTemplateId())
                                                       .serviceTemplateName(null)
                                                       .serviceId(SERVICE_ID)
                                                       .serviceName(SERVICE_NAME)
                                                       .commandName(COMMAND.getName())
                                                       .commandType(COMMAND.getCommandUnitType().name())
                                                       .hostName(HOST_NAME)
                                                       .serviceInstanceId(SERVICE_INSTANCE_ID)
                                                       .build();

  static {
    ACTIVITY_WITH_ID.setAppId(APP_ID);
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }

  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  private static final SimpleWorkflowParam SIMPLE_WORKFLOW_PARAM = aSimpleWorkflowParam().build();
  public static final PhaseElement PHASE_ELEMENT = aPhaseElement().withInfraMappingId(INFRA_MAPPING_ID).build();

  private AbstractCommandUnit commandUnit =
      anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("rm -f $HOME/jetty").build();
  private Command command =
      aCommand()
          .withName(COMMAND_NAME)
          .addCommandUnits(commandUnit)
          .withTemplateVariables(asList(aVariable().withName("var1").withValue("var1Value").build(),
              aVariable().withName("var2").withValue("var2Value").build()))
          .build();
  @Mock ArtifactStream artifactStream;
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
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private HostService hostService;
  @Mock private DelegateService delegateService;
  @Mock private SecretManager secretManager;
  @Mock private InfrastructureMappingService infrastructureMappingService;

  @InjectMocks private CommandState commandState = new CommandState("start1", "START");

  /**
   * Sets up mocks.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUpMocks() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(SERVICE);
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, COMMAND_NAME))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(command).build());
    when(appService.get(APP_ID))
        .thenReturn(anApplication().withUuid(APP_ID).withAccountId(ACCOUNT_ID).withName(APP_NAME).build());
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build());
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(COMMAND).build());
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Start"))
        .thenReturn(aServiceCommand().withCommand(aCommand().withName("Start").build()).build());
    when(serviceResourceService.getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START")).thenReturn(emptyList());
    when(serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID)).thenReturn(SERVICE_INSTANCE);
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);

    when(activityService.get(ACTIVITY_ID, APP_ID)).thenReturn(ACTIVITY_WITH_ID);
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, ENV_ID, CommandState.RUNTIME_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(RUNTIME_PATH).build()).build());
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, ENV_ID, CommandState.BACKUP_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(BACKUP_PATH).build()).build());
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, ENV_ID, CommandState.STAGING_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(STAGING_PATH).build()).build());
    when(settingsService.getByName(ACCOUNT_ID, APP_ID, ENV_ID, WINDOWS_RUNTIME_PATH))
        .thenReturn(aSettingAttribute().withValue(aStringValue().withValue(WINDOWS_RUNTIME_PATH_TEST).build()).build());
    when(settingsService.get(HOST.getHostConnAttr()))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute()
                        .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes().build())
                        .build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(singletonList(SIMPLE_WORKFLOW_PARAM));
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM)).thenReturn(PHASE_ELEMENT);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(aPhysicalInfrastructureMapping()
                        .withAppId(APP_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType("ECS")
                        .build());
    when(context.getContextElement(ContextElementType.SERVICE))
        .thenReturn(aServiceElement().withUuid(SERVICE_ID).build());
    when(context.getContextElement(ContextElementType.INSTANCE))
        .thenReturn(anInstanceElement()
                        .withUuid(SERVICE_INSTANCE_ID)
                        .withServiceTemplateElement(aServiceTemplateElement().withUuid(TEMPLATE_ID).build())
                        .build());
    when(context.renderExpression(anyString())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);
    when(context.getServiceVariables()).thenReturn(emptyMap());
    when(context.getWorkflowId()).thenReturn(UUID.randomUUID().toString());
    ServiceTemplate serviceTemplate = aServiceTemplate().withUuid(TEMPLATE_ID).withServiceId(SERVICE.getUuid()).build();
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);
    when(hostService.getHostByEnv(APP_ID, ENV_ID, HOST_ID)).thenReturn(HOST);
    commandState.setExecutorService(executorService);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(commandState, "secretManager", secretManager);
  }

  /**
   * Execute.
   *
   * @throws Exception the exception
   */
  @Test
  public void execute() {
    when(serviceCommandExecutorService.execute(eq(COMMAND), any())).thenReturn(SUCCESS);

    ExecutionResponse executionResponse = commandState.execute(context);

    when(context.getStateExecutionData()).thenReturn(executionResponse.getStateExecutionData());
    commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, aCommandExecutionResult().withStatus(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).get(APP_ID, SERVICE_ID);

    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);

    verify(activityService).save(any(Activity.class));
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(activityService).getCommandUnits(APP_ID, ACTIVITY_ID);

    verify(delegateService)
        .queueTask(aDelegateTask()
                       .withAppId(APP_ID)
                       .withAccountId(ACCOUNT_ID)
                       .withTaskType(TaskType.COMMAND)
                       .withWaitId(ACTIVITY_ID)
                       .withTimeout(TimeUnit.MINUTES.toMillis(30))
                       .withParameters(new Object[] {COMMAND,
                           aCommandExecutionContext()
                               .withAppId(APP_ID)
                               .withBackupPath(BACKUP_PATH)
                               .withRuntimePath(RUNTIME_PATH)
                               .withStagingPath(STAGING_PATH)
                               .withWindowsRuntimePath(WINDOWS_RUNTIME_PATH_TEST)
                               .withExecutionCredential(null)
                               .withActivityId(ACTIVITY_ID)
                               .withEnvId(ENV_ID)
                               .withHost(HOST)
                               .withServiceTemplateId(TEMPLATE_ID)
                               .withHostConnectionAttributes(
                                   aSettingAttribute()
                                       .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes().build())
                                       .build())
                               .withHostConnectionCredentials(Collections.emptyList())
                               .withBastionConnectionAttributes(
                                   aSettingAttribute()
                                       .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes().build())
                                       .build())
                               .withBastionConnectionCredentials(Collections.emptyList())
                               .withServiceVariables(emptyMap())
                               .withSafeDisplayServiceVariables(emptyMap())
                               .withDeploymentType("ECS")
                               .withAccountId(ACCOUNT_ID)
                               .build()})
                       .withEnvId(ENV_ID)
                       .withInfrastructureMappingId(INFRA_MAPPING_ID)
                       .build());

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(1)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(1)).getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(5)).getWorkflowExecutionId();
    verify(context, times(1)).getWorkflowExecutionName();
    verify(context, times(1)).getWorkflowType();
    verify(context, times(1)).getStateExecutionInstanceId();
    verify(context, times(1)).getStateExecutionInstanceName();
    verify(context, times(1)).getServiceVariables();
    verify(context, times(1)).getSafeDisplayServiceVariables();
    verify(context, times(1)).getWorkflowId();
    verify(context, times(2)).getAppId();
    verify(context).getStateExecutionData();

    verify(context, times(5)).renderExpression(anyString());

    verify(settingsService, times(4)).getByName(eq(ACCOUNT_ID), eq(APP_ID), eq(ENV_ID), anyString());
    verify(settingsService, times(2)).get(anyString());

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));
    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Start");
    verifyNoMoreInteractions(serviceInstanceService, serviceCommandExecutorService, activityService, settingsService,
        workflowExecutionService, artifactStreamService);
  }

  @Test
  public void shouldHandleAsyncResponseWithNoResponse() {
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(context, new HashMap<>());
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  public void shouldFailCommandStateOnErrorResponse() {
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, ErrorNotifyResponseData.builder().errorMessage("Failed").build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  public void shouldHandleCommandException() {
    when(context.getStateExecutionData())
        .thenReturn(CommandStateExecutionData.Builder.aCommandStateExecutionData().build());
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(context,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.Builder.aCommandExecutionResult()
                .withStatus(CommandExecutionStatus.FAILURE)
                .withErrorMessage("Command Failed")
                .build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(activityService, times(2)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
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
                            .withAppId(APP_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(asList(SERVICE_ID))
                            .build();

    ArtifactStreamAttributes artifactStreamAttributes = anArtifactStreamAttributes().withMetadataOnly(false).build();
    Command command =
        aCommand()
            .addCommandUnits(
                ScpCommandUnit.Builder.aScpCommandUnit().withFileCategory(ScpFileCategory.ARTIFACTS).build())
            .build();

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    on(workflowStandardParams).set("artifacts", asList(artifact));
    on(workflowStandardParams).set("appService", appService);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    when(context.getArtifactForService(SERVICE_ID)).thenReturn(artifact);

    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(SERVICE);
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(command).build());

    when(settingsService.get(SETTING_ID))
        .thenReturn(aSettingAttribute()
                        .withValue(JenkinsConfig.builder()
                                       .jenkinsUrl(JENKINS_URL)
                                       .username(USER_NAME)
                                       .password(PASSWORD)
                                       .accountId(ACCOUNT_ID)
                                       .build())
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactStream.getArtifactStreamAttributes()).thenReturn(artifactStreamAttributes);
    when(artifactStream.getSettingId()).thenReturn(SETTING_ID);
    when(artifactStream.getUuid()).thenReturn(ARTIFACT_STREAM_ID);
    when(serviceCommandExecutorService.execute(command,
             aCommandExecutionContext()
                 .withAppId(APP_ID)
                 .withBackupPath(BACKUP_PATH)
                 .withRuntimePath(RUNTIME_PATH)
                 .withStagingPath(STAGING_PATH)
                 .withExecutionCredential(null)
                 .withActivityId(ACTIVITY_ID)
                 .withArtifactStreamAttributes(artifactStreamAttributes)
                 .withArtifactServerEncryptedDataDetails(new ArrayList<>())
                 .build()))
        .thenReturn(SUCCESS);

    ExecutionResponse executionResponse = commandState.execute(context);
    when(context.getStateExecutionData()).thenReturn(executionResponse.getStateExecutionData());
    commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, aCommandExecutionResult().withStatus(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).get(APP_ID, SERVICE_ID);

    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(activityService).save(any(Activity.class));
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");

    verify(delegateService)
        .queueTask(aDelegateTask()
                       .withAppId(APP_ID)
                       .withAccountId(ACCOUNT_ID)
                       .withTaskType(TaskType.COMMAND)
                       .withWaitId(ACTIVITY_ID)
                       .withTimeout(TimeUnit.MINUTES.toMillis(30))
                       .withParameters(new Object[] {command,
                           aCommandExecutionContext()
                               .withAppId(APP_ID)
                               .withBackupPath(BACKUP_PATH)
                               .withRuntimePath(RUNTIME_PATH)
                               .withStagingPath(STAGING_PATH)
                               .withWindowsRuntimePath(WINDOWS_RUNTIME_PATH_TEST)
                               .withExecutionCredential(null)
                               .withActivityId(ACTIVITY_ID)
                               .withEnvId(ENV_ID)
                               .withArtifactFiles(artifact.getArtifactFiles())
                               .withMetadata(artifact.getMetadata())
                               .withHost(HOST)
                               .withServiceTemplateId(TEMPLATE_ID)
                               .withServiceVariables(emptyMap())
                               .withHostConnectionAttributes(
                                   aSettingAttribute()
                                       .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes().build())
                                       .build())
                               .withHostConnectionCredentials(Collections.emptyList())
                               .withBastionConnectionAttributes(
                                   aSettingAttribute()
                                       .withValue(HostConnectionAttributes.Builder.aHostConnectionAttributes().build())
                                       .build())
                               .withBastionConnectionCredentials(Collections.emptyList())
                               .withSafeDisplayServiceVariables(emptyMap())
                               .withDeploymentType("ECS")
                               .withAccountId(ACCOUNT_ID)
                               .withArtifactStreamAttributes(artifactStreamAttributes)
                               .withArtifactServerEncryptedDataDetails(new ArrayList<>())
                               .build()})
                       .withEnvId(ENV_ID)
                       .withInfrastructureMappingId(INFRA_MAPPING_ID)
                       .build());

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(1)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(1)).getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(7)).getWorkflowExecutionId();
    verify(context, times(1)).getWorkflowType();
    verify(context, times(5)).renderExpression(anyString());
    verify(context, times(1)).getWorkflowExecutionName();
    verify(context, times(2)).getStateExecutionInstanceId();
    verify(context, times(1)).getStateExecutionInstanceName();
    verify(context, times(1)).getServiceVariables();
    verify(context, times(1)).getSafeDisplayServiceVariables();
    verify(context, times(1)).getWorkflowId();
    verify(context, times(4)).getAppId();
    verify(context).getStateExecutionData();

    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(settingsService, times(4)).getByName(eq(ACCOUNT_ID), eq(APP_ID), eq(ENV_ID), anyString());
    verify(settingsService, times(3)).get(anyString());

    verify(activityService).getCommandUnits(APP_ID, ACTIVITY_ID);

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verifyNoMoreInteractions(serviceResourceService, serviceInstanceService, activityService,
        serviceCommandExecutorService, settingsService, workflowExecutionService, artifactStreamService);
  }
  /**
   * Execute with artifact.
   *
   * @throws Exception the exception
   */
  @Test
  public void executeFailWhenNoArtifactStreamOrSettingAttribute() throws Exception {
    Artifact artifact = anArtifact()
                            .withUuid(ARTIFACT_ID)
                            .withAppId(APP_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(asList(SERVICE_ID))
                            .build();

    ArtifactStreamAttributes artifactStreamAttributes = anArtifactStreamAttributes().withMetadataOnly(false).build();

    Command command =
        aCommand()
            .addCommandUnits(
                ScpCommandUnit.Builder.aScpCommandUnit().withFileCategory(ScpFileCategory.ARTIFACTS).build())
            .build();

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    on(workflowStandardParams).set("artifacts", asList(artifact));
    on(workflowStandardParams).set("appService", appService);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);

    when(context.getArtifactForService(SERVICE_ID)).thenReturn(artifact);

    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(SERVICE);
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(command).build());

    when(settingsService.get(SETTING_ID))
        .thenReturn(aSettingAttribute()
                        .withValue(JenkinsConfig.builder()
                                       .jenkinsUrl(JENKINS_URL)
                                       .username(USER_NAME)
                                       .password(PASSWORD)
                                       .accountId(ACCOUNT_ID)
                                       .build())
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(null);

    ExecutionResponse executionResponse = commandState.execute(context);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);

    // Now Setting attribute null
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactStream.getArtifactStreamAttributes()).thenReturn(artifactStreamAttributes);
    when(artifactStream.getSettingId()).thenReturn(SETTING_ID);
    when(artifactStream.getUuid()).thenReturn(ARTIFACT_STREAM_ID);
    when(settingsService.get(SETTING_ID)).thenReturn(null);

    executionResponse = commandState.execute(context);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  /**
   * Should throw exception for unknown command.
   */
  @Test
  public void shouldFailWhenNestedCommandNotFound() {
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand()
                        .withTargetToAllEnv(true)
                        .withCommand(aCommand().withName("NESTED_CMD").withReferenceId("NON_EXISTENT_COMMAND").build())
                        .build());

    ExecutionResponse executionResponse = commandState.execute(context);

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).get(APP_ID, SERVICE_ID);
    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "NON_EXISTENT_COMMAND");
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");

    verify(activityService).save(any(Activity.class));
    verify(activityService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);

    verify(context, times(3)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(1)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(5)).getWorkflowExecutionId();
    verify(context, times(1)).getWorkflowExecutionName();
    verify(context, times(1)).getWorkflowType();
    verify(context, times(1)).getStateExecutionInstanceId();
    verify(context, times(1)).getStateExecutionInstanceName();
    verify(context, times(1)).getServiceVariables();
    verify(context, times(1)).getSafeDisplayServiceVariables();

    verify(context, times(5)).renderExpression(anyString());

    verify(settingsService, times(4)).getByName(eq(ACCOUNT_ID), eq(APP_ID), eq(ENV_ID), anyString());
    verify(settingsService, times(2)).get(anyString());
    verify(context, times(1)).getWorkflowId();
    verify(context, times(2)).getAppId();

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementFailed(eq(APP_ID), anyString(), eq(1));

    verifyNoMoreInteractions(serviceResourceService, serviceInstanceService, serviceCommandExecutorService,
        activityService, settingsService, workflowExecutionService, artifactStreamService);
  }

  @Test
  public void shouldRenderCommandString() {
    CommandStateExecutionData commandStateExecutionData =
        CommandStateExecutionData.Builder.aCommandStateExecutionData().build();
    final Command command =
        aCommand()
            .addCommandUnits(anExecCommandUnit().withCommandString("${var1}").build())
            .addCommandUnits(
                aCommand().addCommandUnits(anExecCommandUnit().withCommandString("${var2}").build()).build())
            .build();
    CommandState.renderCommandString(command, context, commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var1}", commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var2}", commandStateExecutionData, null);
  }

  @Test
  public void shouldRenderCommandStringWithVariables() {
    Map<String, Object> stateVariables = new HashMap<>();
    List<Variable> variables = new ArrayList<>();
    variables.add(aVariable().withName("var1").withValue("var1Value").build());
    variables.add(aVariable().withName("var2").withValue("var2Value").build());
    if (isNotEmpty(command.getTemplateVariables())) {
      stateVariables.putAll(
          command.getTemplateVariables().stream().collect(Collectors.toMap(Variable::getName, Variable::getValue)));
    }
    CommandStateExecutionData commandStateExecutionData =
        CommandStateExecutionData.Builder.aCommandStateExecutionData().withTemplateVariable(stateVariables).build();
    final Command command =
        aCommand()
            .addCommandUnits(anExecCommandUnit().withCommandString("${var1}").build())
            .addCommandUnits(
                aCommand().addCommandUnits(anExecCommandUnit().withCommandString("${var2}").build()).build())
            .build();
    CommandState.renderCommandString(command, context, commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var1}", commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var2}", commandStateExecutionData, null);
  }

  @Test
  public void shouldRenderReferencedCommandStringWithVariables() {
    Map<String, Object> stateVariables = new HashMap<>();
    List<Variable> variables = new ArrayList<>();
    variables.add(aVariable().withName("var1").withValue("var1Value").build());
    variables.add(aVariable().withName("var2").withValue("var2Value").build());
    if (isNotEmpty(command.getTemplateVariables())) {
      stateVariables.putAll(
          command.getTemplateVariables().stream().collect(Collectors.toMap(Variable::getName, Variable::getValue)));
    }
    CommandStateExecutionData commandStateExecutionData =
        CommandStateExecutionData.Builder.aCommandStateExecutionData().withTemplateVariable(stateVariables).build();
    final Command command =
        aCommand()
            .addCommandUnits(anExecCommandUnit().withCommandString("${var1}").build())
            .addCommandUnits(aCommand()
                                 .withReferenceId("Start")
                                 .addCommandUnits(anExecCommandUnit().withCommandString("${var2}").build())
                                 .build())
            .build();
    CommandState.renderCommandString(command, context, commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var1}", commandStateExecutionData, null);
    verify(context, times(1)).renderExpression("${var2}", commandStateExecutionData, null);
  }
}
