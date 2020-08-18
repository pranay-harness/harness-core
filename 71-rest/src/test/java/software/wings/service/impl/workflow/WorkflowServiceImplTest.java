package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.Workflow.WorkflowBuilder;
import static software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.WingsBaseTest;
import software.wings.beans.LastDeployedArtifactInformation;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.PipelineSummary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WorkflowServiceImplTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ArtifactService artifactService;
  @InjectMocks @Inject private WorkflowService workflowService;
  @Mock private Query<WorkflowExecution> query;
  @Mock private Query<WorkflowExecution> emptyQuery;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldGetLastDeployedArtifactFromPreviousIndirectExecution() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    List<Artifact> artifacts =
        Arrays.asList(anArtifact()
                          .withUuid(ARTIFACT_ID)
                          .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                          .withArtifactStreamId(ARTIFACT_STREAM_ID)
                          .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                          .build());
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(artifacts)
            .appId(APP_ID)
            .pipelineResumeId(PIPELINE_EXECUTION_ID)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    pageResponse.setResponse(artifacts);
    when(artifactService.listArtifactsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, Arrays.asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNotNull();
    assertThat(artifactInformation.getArtifact().getArtifactSourceName()).isEqualTo(ARTIFACT_SOURCE_NAME);
    assertThat(artifactInformation.getArtifact().getBuildNo()).isEqualTo(BUILD_NO);
    assertThat(artifactInformation.getExecutionId()).isEqualTo(PIPELINE_EXECUTION_ID);
    assertThat(artifactInformation.getExecutionEntityId()).isEqualTo(PIPELINE_ID);
    assertThat(artifactInformation.getExecutionEntityType()).isEqualTo(WorkflowType.PIPELINE);
    assertThat(artifactInformation.getExecutionEntityName()).isEqualTo(PIPELINE_NAME);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfNoSuccessfulExecutionFound() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(emptyQuery);
    when(emptyQuery.order(any(Sort.class))).thenReturn(emptyQuery);
    when(emptyQuery.get()).thenReturn(null);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, Arrays.asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfArtifactStreamChanged() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(Arrays.asList(anArtifact()
                                         .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                                         .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                         .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                                         .build()))
            .status(ExecutionStatus.SUCCESS)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, Arrays.asList(ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnNullIfArtifactModifiedInStream() {
    Workflow workflow =
        WorkflowBuilder.aWorkflow().name(WORKFLOW_NAME).uuid(WORKFLOW_ID).accountId(ACCOUNT_ID).appId(APP_ID).build();

    List<Artifact> artifacts =
        Arrays.asList(anArtifact()
                          .withUuid(ARTIFACT_ID)
                          .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                          .withArtifactStreamId(ARTIFACT_STREAM_ID)
                          .withMetadata(Collections.singletonMap(ArtifactMetadataKeys.buildNo, BUILD_NO))
                          .build());
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder()
            .artifacts(artifacts)
            .appId(APP_ID)
            .pipelineResumeId(PIPELINE_EXECUTION_ID)
            .pipelineExecutionId(PIPELINE_EXECUTION_ID)
            .pipelineSummary(PipelineSummary.builder().pipelineId(PIPELINE_ID).pipelineName(PIPELINE_NAME).build())
            .build();

    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.accountId, workflow.getAccountId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.appId, workflow.getAppId())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())).thenReturn(query);
    when(query.filter(WorkflowExecutionKeys.status, SUCCESS)).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    PageResponse<Artifact> pageResponse = new PageResponse<>();
    when(artifactService.listArtifactsForService(APP_ID, SERVICE_ID, new PageRequest<>())).thenReturn(pageResponse);
    WorkflowServiceImpl workflowServiceImpl = (WorkflowServiceImpl) workflowService;
    LastDeployedArtifactInformation artifactInformation = workflowServiceImpl.fetchLastDeployedArtifact(
        workflow, Arrays.asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID_ARTIFACTORY), SERVICE_ID);
    assertThat(artifactInformation).isNull();
  }
}
