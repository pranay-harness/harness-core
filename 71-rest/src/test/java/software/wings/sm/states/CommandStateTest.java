package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.api.SimpleWorkflowParam.Builder.aSimpleWorkflowParam;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.StringValue.Builder.aStringValue;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
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

import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskBuilder;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.OwnerRule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.Builder;
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
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.command.TailFilePatternEntry;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.template.TemplateUtils;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

  private static WorkflowStandardParams workflowStandardParams =
      aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
  private static final SimpleWorkflowParam SIMPLE_WORKFLOW_PARAM = aSimpleWorkflowParam().build();
  private static final PhaseElement PHASE_ELEMENT = aPhaseElement().withInfraMappingId(INFRA_MAPPING_ID).build();
  private static final String PHASE_PARAM = "PHASE_PARAM";
  private static final String WINDOWS_RUNTIME_PATH = "WINDOWS_RUNTIME_PATH";

  private AbstractCommandUnit commandUnit =
      anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("rm -f $HOME/jetty").build();
  private Command command = aCommand()
                                .withName(COMMAND_NAME)
                                .addCommandUnits(commandUnit)
                                .withTemplateVariables(asList(aVariable().name("var1").value("var1Value").build(),
                                    aVariable().name("var2").value("var2Value").build()))
                                .build();
  @Mock ArtifactStream artifactStream;
  @Inject private ExecutorService executorService;

  @Mock private AppService appService;
  @Mock private ExecutionContextImpl context;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceCommandExecutorService serviceCommandExecutorService;
  @Mock private ActivityHelperService activityHelperService;
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
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AwsCommandHelper mockAwsCommandHelper;
  @Mock private TemplateUtils templateUtils;

  @InjectMocks private CommandState commandState = new CommandState("start1", "START");

  /**
   * Sets up mocks.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUpMocks() throws IllegalAccessException {
    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(SERVICE);
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, COMMAND_NAME))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(command).build());
    when(appService.get(APP_ID)).thenReturn(anApplication().uuid(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build());
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build());
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand().withTargetToAllEnv(true).withCommand(COMMAND).build());
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Start"))
        .thenReturn(aServiceCommand().withCommand(aCommand().withName("Start").build()).build());
    when(serviceResourceService.getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START")).thenReturn(emptyList());
    when(serviceInstanceService.get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID)).thenReturn(SERVICE_INSTANCE);
    when(serviceResourceService.getDeploymentType(any(), any(), any())).thenReturn(DeploymentType.ECS);
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
    workflowStandardParams.setCurrentUser(EmbeddedUser.builder().name("test").email("test@harness.io").build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(singletonList(SIMPLE_WORKFLOW_PARAM));
    when(context.getContextElement(ContextElementType.PARAM, PHASE_PARAM)).thenReturn(PHASE_ELEMENT);
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
    doReturn(null).when(mockAwsCommandHelper).getAwsConfigTagsFromContext(any());
    FieldUtils.writeField(commandState, "secretManager", secretManager, true);

    when(activityHelperService.createAndSaveActivity(any(ExecutionContext.class), any(Activity.Type.class), anyString(),
             anyString(), anyList(), any(Artifact.class)))
        .thenReturn(ACTIVITY_WITH_ID);
  }

  /**
   * Execute.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void execute() {
    when(serviceCommandExecutorService.execute(eq(COMMAND), any())).thenReturn(SUCCESS);

    ExecutionResponse executionResponse = commandState.execute(context);

    when(context.getStateExecutionData()).thenReturn(executionResponse.getStateExecutionData());
    commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, CommandExecutionResult.builder().status(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getWithDetails(APP_ID, SERVICE_ID);

    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);

    verify(activityHelperService)
        .createAndSaveActivity(any(ExecutionContext.class), any(Activity.Type.class), anyString(), anyString(),
            anyList(), any(Artifact.class));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);

    verify(delegateService)
        .queueTask(
            DelegateTask.builder()
                .async(true)
                .appId(APP_ID)
                .accountId(ACCOUNT_ID)
                .waitId(ACTIVITY_ID)
                .data(
                    TaskData.builder()
                        .taskType(TaskType.COMMAND.name())
                        .parameters(new Object[] {COMMAND,
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
                        .timeout(TimeUnit.MINUTES.toMillis(30))
                        .build())
                .envId(ENV_ID)
                .infrastructureMappingId(INFRA_MAPPING_ID)
                .build());

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(1)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(1)).getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(1)).getServiceVariables();
    verify(context, times(1)).getSafeDisplayServiceVariables();
    verify(context, times(2)).getAppId();
    verify(context).getStateExecutionData();

    verify(context, times(5)).renderExpression(anyString());

    verify(settingsService, times(4)).getByName(eq(ACCOUNT_ID), eq(APP_ID), eq(ENV_ID), anyString());
    verify(settingsService, times(2)).get(anyString());

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));
    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "Start");
    verifyNoMoreInteractions(serviceInstanceService, serviceCommandExecutorService, activityHelperService,
        settingsService, workflowExecutionService, artifactStreamService);
    verify(activityService).getCommandUnits(APP_ID, ACTIVITY_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponseWithNoResponse() {
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(context, new HashMap<>());
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailCommandStateOnErrorResponse() {
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(
        context, ImmutableMap.of(ACTIVITY_ID, ErrorNotifyResponseData.builder().errorMessage("Failed").build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleCommandException() {
    when(context.getStateExecutionData())
        .thenReturn(CommandStateExecutionData.Builder.aCommandStateExecutionData().build());
    ExecutionResponse executionResponse = commandState.handleAsyncResponse(context,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.builder()
                .status(CommandExecutionStatus.FAILURE)
                .errorMessage("Command Failed")
                .build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    verify(activityHelperService, times(2)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
  }

  /**
   * Execute with artifact.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(emails = SRINIVAS)
  @Category(UnitTests.class)
  public void executeWithArtifact() throws Exception {
    Artifact artifact =
        anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build();

    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder().metadataOnly(false).build();
    Command command =
        aCommand()
            .addCommandUnits(
                ScpCommandUnit.Builder.aScpCommandUnit().withFileCategory(ScpFileCategory.ARTIFACTS).build())
            .build();

    setWorkflowStandardParams(artifact, command);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactStream.fetchArtifactStreamAttributes()).thenReturn(artifactStreamAttributes);
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
        context, ImmutableMap.of(ACTIVITY_ID, CommandExecutionResult.builder().status(SUCCESS).build()));

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getWithDetails(APP_ID, SERVICE_ID);

    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(activityHelperService)
        .createAndSaveActivity(any(ExecutionContext.class), any(Activity.Type.class), anyString(), anyString(),
            anyList(), any(Artifact.class));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getDeploymentType(any(), any(), any());

    DelegateTaskBuilder delegateBuilder = getDelegateBuilder(artifact, artifactStreamAttributes, command);
    DelegateTask delegateTask = delegateBuilder.build();

    delegateService.queueTask(delegateTask);

    verify(context, times(4)).getContextElement(ContextElementType.STANDARD);
    verify(context, times(1)).getContextElement(ContextElementType.INSTANCE);
    verify(context, times(1)).getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    verify(context, times(2)).getContextElementList(ContextElementType.PARAM);
    verify(context, times(5)).renderExpression(anyString());
    verify(context, times(1)).getServiceVariables();
    verify(context, times(1)).getSafeDisplayServiceVariables();
    verify(context, times(4)).getAppId();
    verify(context).getStateExecutionData();

    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(settingsService, times(4)).getByName(eq(ACCOUNT_ID), eq(APP_ID), eq(ENV_ID), anyString());
    verify(settingsService, times(3)).get(anyString());

    verify(workflowExecutionService).incrementInProgressCount(eq(APP_ID), anyString(), eq(1));
    verify(workflowExecutionService).incrementSuccess(eq(APP_ID), anyString(), eq(1));
    verify(artifactStreamService).get(ARTIFACT_STREAM_ID);
    verifyNoMoreInteractions(serviceResourceService, serviceInstanceService, activityHelperService,
        serviceCommandExecutorService, settingsService, workflowExecutionService, artifactStreamService);
    verify(activityService).getCommandUnits(APP_ID, ACTIVITY_ID);
  }

  @NotNull
  private DelegateTaskBuilder getDelegateBuilder(
      Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes, Command command) {
    CommandExecutionContext commandExecutionContext =
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
            .withServiceVariables(emptyMap())
            .withHostConnectionAttributes(
                aSettingAttribute().withValue(Builder.aHostConnectionAttributes().build()).build())
            .withHostConnectionCredentials(Collections.emptyList())
            .withBastionConnectionAttributes(
                aSettingAttribute().withValue(Builder.aHostConnectionAttributes().build()).build())
            .withBastionConnectionCredentials(Collections.emptyList())
            .withSafeDisplayServiceVariables(emptyMap())
            .withDeploymentType("ECS")
            .withAccountId(ACCOUNT_ID)
            .withArtifactStreamAttributes(artifactStreamAttributes)
            .withArtifactServerEncryptedDataDetails(new ArrayList<>())
            .build();
    DelegateTaskBuilder builder = DelegateTask.builder()
                                      .appId(APP_ID)
                                      .accountId(ACCOUNT_ID)
                                      .waitId(ACTIVITY_ID)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.COMMAND.name())
                                                .parameters(new Object[] {command, commandExecutionContext})
                                                .timeout(TimeUnit.MINUTES.toMillis(30))
                                                .build());

    if (artifact != null) {
      commandExecutionContext.setArtifactFiles(artifact.getArtifactFiles());
      commandExecutionContext.setMetadata(artifact.getMetadata());
    }
    return builder.envId(ENV_ID).infrastructureMappingId(INFRA_MAPPING_ID);
  }

  /**
   * Execute with artifact.
   *
   * @throws Exception the exception
   */
  @Test
  @Category(UnitTests.class)
  public void executeFailWhenNoArtifactStreamOrSettingAttribute() throws Exception {
    Artifact artifact =
        anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build();

    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder().metadataOnly(false).build();

    Command command =
        aCommand()
            .addCommandUnits(
                ScpCommandUnit.Builder.aScpCommandUnit().withFileCategory(ScpFileCategory.ARTIFACTS).build())
            .build();

    setWorkflowStandardParams(artifact, command);

    ExecutionResponse executionResponse = commandState.execute(context);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);

    // Now Setting attribute null
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactStream.fetchArtifactStreamAttributes()).thenReturn(artifactStreamAttributes);
    when(artifactStream.getSettingId()).thenReturn(SETTING_ID);
    when(artifactStream.getUuid()).thenReturn(ARTIFACT_STREAM_ID);
    when(settingsService.get(SETTING_ID)).thenReturn(null);

    executionResponse = commandState.execute(context);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  private void setWorkflowStandardParams(Artifact artifact, Command command) {
    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    on(workflowStandardParams).set("artifacts", asList(artifact));
    on(workflowStandardParams).set("appService", appService);

    when(context.getArtifactForService(SERVICE_ID)).thenReturn(artifact);

    when(serviceResourceService.getWithDetails(APP_ID, SERVICE_ID)).thenReturn(SERVICE);
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
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(null);
  }

  /**
   * Should throw exception for unknown command.
   */
  @Test
  @Owner(emails = "aaditi@harness.io", intermittent = true)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldFailWhenNestedCommandNotFound() {
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START"))
        .thenReturn(aServiceCommand()
                        .withTargetToAllEnv(true)
                        .withCommand(aCommand().withName("NESTED_CMD").withReferenceId("NON_EXISTENT_COMMAND").build())
                        .build());

    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getWithDetails(APP_ID, SERVICE_ID);
    verify(serviceInstanceService).get(APP_ID, ENV_ID, SERVICE_INSTANCE_ID);
    verify(serviceResourceService).getCommandByName(APP_ID, SERVICE_ID, ENV_ID, "NON_EXISTENT_COMMAND");
    verify(serviceResourceService).getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, "START");
    verify(serviceResourceService).getDeploymentType(any(), any(), any());

    verify(activityHelperService)
        .createAndSaveActivity(any(ExecutionContext.class), any(Activity.Type.class), anyString(), anyString(),
            anyList(), any(Artifact.class));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);

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
        activityHelperService, settingsService, workflowExecutionService, artifactStreamService);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRenderCommandString() {
    CommandState commandState = new CommandState("test");
    on(commandState).set("templateUtils", templateUtils);
    CommandStateExecutionData commandStateExecutionData =
        CommandStateExecutionData.Builder.aCommandStateExecutionData().build();
    final Command command =
        aCommand()
            .addCommandUnits(anExecCommandUnit().withCommandString("${var1}").build())
            .addCommandUnits(
                aCommand().addCommandUnits(anExecCommandUnit().withCommandString("${var2}").build()).build())
            .build();
    Artifact artifact = null;
    commandState.renderCommandString(command, context, commandStateExecutionData, artifact);
    verify(context, times(1))
        .renderExpression(
            "${var1}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
    verify(context, times(1))
        .renderExpression(
            "${var2}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRenderTailFilesPatterns() {
    CommandState commandState = new CommandState("test");
    on(commandState).set("templateUtils", templateUtils);
    CommandStateExecutionData commandStateExecutionData =
        CommandStateExecutionData.Builder.aCommandStateExecutionData().build();
    ExecCommandUnit execCommandUnit = anExecCommandUnit()
                                          .withCommandString("echo \"Hello World\"")
                                          .withTailPatterns(asList(TailFilePatternEntry.Builder.aTailFilePatternEntry()
                                                                       .withFilePath("${serviceVariable.testfile}")
                                                                       .withPattern("${serviceVariable.filepattern}")
                                                                       .build()))
                                          .build();
    Artifact artifact = null;
    commandState.renderTailFilePattern(context, commandStateExecutionData, artifact, execCommandUnit);
    verify(context, times(1))
        .renderExpression("${serviceVariable.testfile}",
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
    verify(context, times(1))
        .renderExpression("${serviceVariable.filepattern}",
            StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRenderCommandStringWithVariables() {
    CommandState commandState = new CommandState("test");
    on(commandState).set("templateUtils", templateUtils);
    Map<String, Object> stateVariables = new HashMap<>();
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
    Artifact artifact = null;
    commandState.renderCommandString(command, context, commandStateExecutionData, artifact);
    verify(context, times(1))
        .renderExpression(
            "${var1}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
    verify(context, times(1))
        .renderExpression(
            "${var2}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRenderReferencedCommandStringWithVariables() {
    CommandState commandState = new CommandState("test");
    on(commandState).set("templateUtils", templateUtils);
    Map<String, Object> stateVariables = new HashMap<>();
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
    Artifact artifact = null;
    commandState.renderCommandString(command, context, commandStateExecutionData, artifact);
    verify(context, times(1))
        .renderExpression(
            "${var1}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
    verify(context, times(1))
        .renderExpression(
            "${var2}", StateExecutionContext.builder().stateExecutionData(commandStateExecutionData).build());
  }
}
