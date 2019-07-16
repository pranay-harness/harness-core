package software.wings.service.impl.trigger;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.trigger.TriggerArtifactVariable.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.TriggerArtifactVariable.Type.LAST_COLLECTED;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.artifact;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildJenkinsArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflow;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.distribution.idempotence.IdempotentLock;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionArtifact;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.service.impl.trigger.TriggerServiceImpl.TriggerIdempotentResult;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;

import java.util.ArrayList;
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

  @Inject @InjectMocks private DeploymentTriggerService deploymentTriggerService;
  @Inject @InjectMocks private DeploymentTriggerServiceHelper deploymentTriggerServiceHelper;
  @Inject @InjectMocks private ArtifactTriggerProcessor artifactTriggerProcessor;
  @Inject private DeploymentTriggerGenerator deploymentTriggerGenerator;

  JenkinsArtifactStream artifactStream = buildJenkinsArtifactStream();
  @Before
  public void setUp() {
    List<ServiceVariable> serviceVariableList = asList(
        ServiceVariable.builder().type(Type.ARTIFACT).name(VARIABLE_NAME).value(VARIABLE_VALUE.toCharArray()).build());
    Pipeline pipeline = buildPipeline();
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, true))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, false)).thenReturn(pipeline);
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());

    when(artifactService.getArtifactByBuildNumber(artifactStream, ARTIFACT_FILTER, false)).thenReturn(artifact);
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    when(serviceResourceService.get(ENTITY_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());

    when(idempotentRegistry.create(any(), any(), any(), any()))
        .thenReturn(IdempotentLock.<TriggerIdempotentResult>builder()
                        .registry(idempotentRegistry)
                        .resultData(Optional.empty())
                        .build());
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build());
    on(deploymentTriggerServiceHelper).set("serviceVariablesService", serviceVariablesService);
    when(serviceVariablesService.getServiceVariablesForEntity(APP_ID, ENTITY_ID, OBTAIN_VALUE))
        .thenReturn(serviceVariableList);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldCRUDArtifactConditionTrigger() {
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).build());

    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .name("New Artifact Pipeline")
            .action(PipelineAction.builder().pipelineId(PIPELINE_ID).triggerArgs(TriggerArgs.builder().build()).build())
            .condition(ArtifactCondition.builder().artifactStreamId(ARTIFACT_STREAM_ID).build())
            .build();

    DeploymentTrigger savedTrigger =
        deploymentTriggerService.save(deploymentTriggerGenerator.ensureDeploymentTrigger(trigger));

    assertThat(savedTrigger).isNotNull();
    PipelineAction pipelineAction = (PipelineAction) (savedTrigger.getAction());
    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    assertThat(((ArtifactCondition) savedTrigger.getCondition()).getArtifactSourceName())
        .isNotNull()
        .isEqualTo(ARTIFACT_SOURCE_NAME);

    savedTrigger = deploymentTriggerService.get(savedTrigger.getAppId(), savedTrigger.getUuid());

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
  @Category(UnitTests.class)
  public void shouldExecuteArtifactTriggerPostArtifactCollection() {
    Artifact artifact = prepareArtifact(ARTIFACT_ID);

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
                           .artifactStreamId(ARTIFACT_STREAM_ID)
                           .artifactFilter(ARTIFACT_FILTER)
                           .build())
            .build();

    DeploymentTrigger savedTrigger = deploymentTriggerService.save(trigger);
    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        ACCOUNT_ID, APP_ID, ARTIFACT_STREAM_ID, asList(artifact));

    verify(workflowExecutionService)
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class), any(Trigger.class));
  }

  private List<TriggerArtifactVariable> buildArtifactVariables() {
    List<TriggerArtifactVariable> triggerArtifactVariables = new ArrayList<>();

    triggerArtifactVariables.add(TriggerArtifactVariable.builder()
                                     .type(ARTIFACT_SOURCE)
                                     .variableName(VARIABLE_NAME)
                                     .variableValue(null)
                                     .entityId(ENTITY_ID)
                                     .entityType(EntityType.SERVICE)
                                     .build());

    triggerArtifactVariables.add(
        TriggerArtifactVariable.builder()
            .type(LAST_COLLECTED)
            .variableName(VARIABLE_NAME)
            .variableValue(TriggerArtifactSelectionArtifact.builder().artifactStreamId(ARTIFACT_STREAM_ID).build())
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