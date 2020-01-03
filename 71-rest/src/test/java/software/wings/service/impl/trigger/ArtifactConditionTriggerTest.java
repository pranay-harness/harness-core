package software.wings.service.impl.trigger;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.wings.beans.AzureInfrastructureMapping.Builder.anAzureInfrastructureMapping;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.trigger.TriggerLastDeployedType.WORKFLOW;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.artifact;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildJenkinsArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflow;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACTORY_URL;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.INFRA_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionFromSource;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactSelectionLastDeployed;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.service.impl.trigger.TriggerServiceImpl.TriggerIdempotentResult;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ArtifactConditionTriggerTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowService workflowService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private ServiceVariableService serviceVariablesService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactService artifactService;
  @Mock private MongoIdempotentRegistry<TriggerIdempotentResult> idempotentRegistry;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;
  @Mock private SettingsService settingsService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infrastructureMappingService;

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private DeploymentTriggerServiceHelper deploymentTriggerServiceHelper;
  @Inject @InjectMocks private TriggerDeploymentExecution triggerDeploymentExecution;
  @Inject @InjectMocks private TriggerArtifactVariableHandler triggerArtifactVariableHandler;
  @Inject @InjectMocks private ArtifactTriggerProcessor artifactTriggerProcessor;
  @Inject private DeploymentTriggerGenerator deploymentTriggerGenerator;

  JenkinsArtifactStream jenkinsArtifactStream = buildJenkinsArtifactStream();

  List<TriggerArtifactVariable> triggerArtifactVariables = buildArtifactVariables();
  DeploymentTrigger trigger =
      DeploymentTrigger.builder()
          .name("Artifact Pipeline")
          .appId(APP_ID)
          .action(PipelineAction.builder()
                      .pipelineId(PIPELINE_ID)
                      .triggerArgs(TriggerArgs.builder().triggerArtifactVariables(triggerArtifactVariables).build())
                      .build())
          .condition(ArtifactCondition.builder()
                         .artifactServerId(SETTING_ID)
                         .artifactStreamId(ARTIFACT_STREAM_ID)
                         .artifactFilter("^release")
                         .build())
          .build();

  List<ArtifactVariable> variables = singletonList(ArtifactVariable.builder()
                                                       .name(VARIABLE_NAME)
                                                       .entityId(ENTITY_ID)
                                                       .type(VariableType.ARTIFACT)
                                                       .value(ARTIFACT_ID)
                                                       .build());

  Pipeline pipeline = buildPipeline();
  @Before
  public void setUp() {
    SettingAttribute artifactorySetting = aSettingAttribute()
                                              .withUuid(SETTING_ID)
                                              .withName(SETTING_NAME)
                                              .withValue(ArtifactoryConfig.builder()
                                                             .artifactoryUrl(ARTIFACTORY_URL)
                                                             .username("admin")
                                                             .password("dummy123!".toCharArray())
                                                             .build())
                                              .build();
    List<ServiceVariable> serviceVariableList = asList(
        ServiceVariable.builder().type(Type.ARTIFACT).name(VARIABLE_NAME).value(VARIABLE_VALUE.toCharArray()).build());

    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.readPipelineWithResolvedVariables(APP_ID, PIPELINE_ID, new HashMap<>())).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactStreamService.get("INVALID_STREAM_ID")).thenReturn(null);
    when(artifactStreamServiceBindingService.listServiceIds(ARTIFACT_STREAM_ID)).thenReturn(asList(SERVICE_ID));
    when(artifactStreamServiceBindingService.listServiceIds(ARTIFACT_ID)).thenReturn(asList(SERVICE_ID));
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, true))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, false)).thenReturn(pipeline);
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(environmentService.get(APP_ID, ENV_ID)).thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAzureInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withName(INFRA_NAME).build());
    when(artifactService.get(ARTIFACT_ID)).thenReturn(prepareArtifact(ARTIFACT_ID));
    when(artifactService.getArtifactByBuildNumber(jenkinsArtifactStream, ARTIFACT_FILTER, false)).thenReturn(artifact);
    when(idempotentRegistry.create(any(), any(), any(), any()))
        .thenReturn(IdempotentLock.<TriggerIdempotentResult>builder()
                        .registry(idempotentRegistry)
                        .resultData(Optional.empty())
                        .build());
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    when(settingsService.get(SETTING_ID)).thenReturn(artifactorySetting);
    when(settingsService.get("INVALID_SETTING_ID")).thenThrow(new TriggerException("Invalid Setting Id", null));

    when(serviceResourceService.get(ENTITY_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());

    on(triggerArtifactVariableHandler).set("serviceVariablesService", serviceVariablesService);
    on(triggerDeploymentExecution).set("serviceVariablesService", serviceVariablesService);
    when(serviceVariablesService.getServiceVariablesForEntity(APP_ID, ENTITY_ID, OBTAIN_VALUE))
        .thenReturn(serviceVariableList);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCRUDArtifactConditionTrigger() {
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).build());

    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .name("New Artifact Pipeline")
            .action(PipelineAction.builder().pipelineId(PIPELINE_ID).triggerArgs(TriggerArgs.builder().build()).build())
            .condition(
                ArtifactCondition.builder().artifactServerId(SETTING_ID).artifactStreamId(ARTIFACT_STREAM_ID).build())
            .build();

    DeploymentTrigger savedTrigger =
        deploymentTriggerService.save(deploymentTriggerGenerator.ensureDeploymentTrigger(trigger), false);

    assertThat(savedTrigger).isNotNull();
    PipelineAction pipelineAction = (PipelineAction) (savedTrigger.getAction());
    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    savedTrigger = deploymentTriggerService.get(savedTrigger.getAppId(), savedTrigger.getUuid(), false);

    assertThat(savedTrigger).isNotNull();
    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    savedTrigger.setDescription(TRIGGER_DESCRIPTION);

    DeploymentTrigger updatedTrigger = deploymentTriggerService.update(savedTrigger);

    assertThat(updatedTrigger).isNotNull();
    assertThat(updatedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());
    assertThat(updatedTrigger.getDescription()).isEqualTo(TRIGGER_DESCRIPTION);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteArtifactTriggerPostArtifactCollection() {
    Artifact artifact = prepareArtifact(ARTIFACT_ID);

    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .name("Artifact Pipeline")
            .appId(APP_ID)
            .action(PipelineAction.builder()
                        .pipelineId(PIPELINE_ID)
                        .triggerArgs(TriggerArgs.builder().triggerArtifactVariables(triggerArtifactVariables).build())
                        .build())
            .condition(ArtifactCondition.builder()
                           .artifactStreamId(ARTIFACT_STREAM_ID)
                           .artifactServerId(SETTING_ID)
                           .artifactFilter(ARTIFACT_FILTER)
                           .build())
            .build();

    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, PIPELINE_ID, new HashMap<>(), null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());
    when(pipelineService.fetchDeploymentMetadata(APP_ID, pipeline, null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());

    DeploymentTrigger savedTrigger = deploymentTriggerService.save(trigger, false);
    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    Mockito.verify(workflowExecutionService)
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWithArtifactStreamId() {
    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .name("Artifact Pipeline1")
            .appId(APP_ID)
            .action(PipelineAction.builder()
                        .pipelineId(PIPELINE_ID)
                        .triggerArgs(TriggerArgs.builder().triggerArtifactVariables(triggerArtifactVariables).build())
                        .build())
            .condition(ArtifactCondition.builder()
                           .artifactStreamId(ARTIFACT_STREAM_ID)
                           .artifactServerId(SETTING_ID)
                           .artifactFilter(ARTIFACT_FILTER)
                           .build())
            .build();

    Artifact artifact = prepareArtifact(ARTIFACT_ID);

    deploymentTriggerService.save(trigger, false);

    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, PIPELINE_ID, new HashMap<>(), null, null, Include.ARTIFACT_SERVICE))
        .thenThrow(new TriggerException("Invalid input", null));

    assertThatExceptionOfType(TriggerException.class)
        .isThrownBy(()
                        -> deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
                            ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact)));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerExecutionPostArtifactCollectionWithFileNotMatchesArtifactFilter() {
    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .withArtifactFiles(singletonList(
                                anArtifactFile().withAppId(APP_ID).withFileUuid(FILE_ID).withName(FILE_NAME).build()))
                            .build();

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    Mockito.verify(workflowExecutionService, times(0))
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerExecutionPostArtifactCollectionWithFileRegexNotStartsWith() {
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactServerId(SETTING_ID)
                             .artifactStreamId(ARTIFACT_STREAM_ID)
                             .artifactFilter("^(?!release)")
                             .build());

    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .withArtifactFiles(singletonList(
                                anArtifactFile().withAppId(APP_ID).withFileUuid(FILE_ID).withName(FILE_NAME).build()))
                            .build();

    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, PIPELINE_ID, new HashMap<>(), null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());
    when(pipelineService.fetchDeploymentMetadata(APP_ID, pipeline, null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());

    DeploymentTrigger savedTrigger = deploymentTriggerService.save(trigger, false);

    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    Mockito.verify(workflowExecutionService)
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerExecutionPostArtifactCollectionRegexDoesNotMatch() {
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId(ARTIFACT_STREAM_ID)
                             .artifactServerId(SETTING_ID)
                             .artifactFilter("^release")
                             .build());

    deploymentTriggerService.save(trigger, false);

    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", "@33release23"))
                            .build();

    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    Mockito.verify(workflowExecutionService, times(0))
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerPostArtifactCollectionForAllArtifactsMatch() {
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactServerId(SETTING_ID)
                             .artifactStreamId(ARTIFACT_STREAM_ID)
                             .artifactFilter("^release")
                             .build());

    deploymentTriggerService.save(trigger, false);
    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, PIPELINE_ID, new HashMap<>(), null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());
    when(pipelineService.fetchDeploymentMetadata(APP_ID, pipeline, null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());

    when(featureFlagService.isEnabled(FeatureName.TRIGGER_FOR_ALL_ARTIFACTS, ACCOUNT_ID)).thenReturn(true);
    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", "release23"))
                            .build();
    Artifact artifact2 = anArtifact()
                             .withAppId(APP_ID)
                             .withUuid(ARTIFACT_ID)
                             .withArtifactStreamId(ARTIFACT_STREAM_ID)
                             .withMetadata(ImmutableMap.of("buildNo", "release456"))
                             .build();

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact, artifact2));

    Mockito.verify(workflowExecutionService, times(2))
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerPostArtifactCollectionOneArtifactMatchesOtherDoesNotMatch() {
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);

    when(pipelineService.fetchDeploymentMetadata(
             APP_ID, PIPELINE_ID, new HashMap<>(), null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());
    when(pipelineService.fetchDeploymentMetadata(APP_ID, pipeline, null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());

    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    deploymentTriggerService.save(trigger, false);

    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", "@33release23"))
                            .build();
    Artifact artifact2 = anArtifact()
                             .withAppId(APP_ID)
                             .withUuid(ARTIFACT_ID)
                             .withArtifactStreamId(ARTIFACT_STREAM_ID)
                             .withMetadata(ImmutableMap.of("buildNo", "release456"))
                             .build();

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact, artifact2));

    Mockito.verify(workflowExecutionService)
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTriggerTemplateWorkflowExecution() {
    Workflow workflow = buildWorkflow();
    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().name("Environment").value(ENV_ID).entityType(ENVIRONMENT).build());

    final List<Variable> triggerVariables = asList(
        aVariable().name("Environment").value(ENV_ID).entityType(ENVIRONMENT).build(),
        aVariable().name("Service").value(SERVICE_ID).entityType(SERVICE).build(),
        aVariable().name("ServiceInfraStructure").value(INFRA_MAPPING_ID).entityType(INFRASTRUCTURE_MAPPING).build());

    final ImmutableMap<String, String> triggerVariables1 =
        ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID, "ServiceInfraStructure", INFRA_MAPPING_ID);

    DeploymentTrigger trigger = DeploymentTrigger.builder()
                                    .name("Artifact Pipeline")
                                    .appId(APP_ID)
                                    .action(WorkflowAction.builder()
                                                .workflowId(WORKFLOW_ID)
                                                .triggerArgs(TriggerArgs.builder()
                                                                 .variables(triggerVariables)
                                                                 .triggerArtifactVariables(triggerArtifactVariables)
                                                                 .build())
                                                .build())
                                    .condition(ArtifactCondition.builder()
                                                   .artifactStreamId(ARTIFACT_STREAM_ID)
                                                   .artifactServerId(SETTING_ID)
                                                   .artifactFilter(ARTIFACT_FILTER)
                                                   .build())
                                    .build();

    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());

    deploymentTriggerService.save(trigger, false);

    when(workflowExecutionService.triggerEnvExecution(
             anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class)))
        .thenReturn(WorkflowExecution.builder().appId(APP_ID).status(SUCCESS).build());

    ExecutionArgs executionArgs = new ExecutionArgs();

    List<ArtifactVariable> variables = singletonList(ArtifactVariable.builder()
                                                         .name(VARIABLE_NAME)
                                                         .entityId(ENTITY_ID)
                                                         .type(VariableType.ARTIFACT)
                                                         .value(ARTIFACT_ID)
                                                         .build());
    executionArgs.setArtifactVariables(variables);
    executionArgs.setArtifacts(singletonList(artifact));
    executionArgs.setWorkflowVariables(ImmutableMap.of("MyVar", "MyVal"));

    when(workflowService.fetchDeploymentMetadata(
             APP_ID, workflow, triggerVariables1, null, null, Include.ARTIFACT_SERVICE))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactVariables(variables)
                        .artifactRequiredServiceIds(asList(SERVICE_ID))
                        .build());

    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            WorkflowExecution.builder().appId(APP_ID).executionArgs(executionArgs).build()))
                        .build());

    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    Mockito.verify(workflowExecutionService)
        .triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
    Mockito.verify(workflowService, times(2)).readWorkflow(APP_ID, WORKFLOW_ID);
    Mockito.verify(workflowService, times(2)).fetchWorkflowName(APP_ID, WORKFLOW_ID);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowTriggerExceptionForInvalidSettingId() {
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId("INVALID_STREAM_ID")
                             .artifactServerId("INVALID_SETTING_ID")
                             .artifactFilter("^release")
                             .build());

    assertThatExceptionOfType(WingsException.class).isThrownBy(() -> deploymentTriggerService.save(trigger, false));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldThrowTriggerExceptionForInvalidFilter() {
    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId("INVALID_STREAM_ID")
                             .artifactServerId("INVALID_SETTING_ID")
                             .artifactFilter("dlk*,release")
                             .build());

    assertThatExceptionOfType(WingsException.class).isThrownBy(() -> deploymentTriggerService.save(trigger, false));
  }

  private List<TriggerArtifactVariable> buildArtifactVariables() {
    List<TriggerArtifactVariable> triggerArtifactVariables = new ArrayList<>();

    triggerArtifactVariables.add(TriggerArtifactVariable.builder()
                                     .variableName(VARIABLE_NAME)
                                     .variableValue(TriggerArtifactSelectionFromSource.builder().build())
                                     .entityId(ENTITY_ID)
                                     .entityType(EntityType.SERVICE)
                                     .build());

    triggerArtifactVariables.add(TriggerArtifactVariable.builder()
                                     .variableName(VARIABLE_NAME)
                                     .variableValue(TriggerArtifactSelectionLastCollected.builder()
                                                        .artifactServerId(SETTING_ID)
                                                        .artifactStreamId(ARTIFACT_STREAM_ID)
                                                        .build())
                                     .entityId(ENTITY_ID)
                                     .entityType(EntityType.SERVICE)
                                     .build());

    triggerArtifactVariables.add(
        TriggerArtifactVariable.builder()
            .variableName(VARIABLE_NAME)
            .variableValue(TriggerArtifactSelectionLastDeployed.builder().id(WORKFLOW_ID).type(WORKFLOW).build())
            .entityId(ENTITY_ID)
            .entityType(EntityType.SERVICE)
            .build());
    return triggerArtifactVariables;
  }

  private Artifact prepareArtifact(String artifactId) {
    return anArtifact()
        .withAppId(APP_ID)
        .withUuid(artifactId)
        .withArtifactStreamId(ARTIFACT_STREAM_ID)
        .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
        .build();
  }
}