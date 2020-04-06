package software.wings.service.impl.pipeline.resume;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.APPROVAL_RESUME;
import static software.wings.sm.StateType.ENV_RESUME_STATE;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.ApprovalResumeState.ApprovalResumeStateKeys;
import software.wings.sm.states.EnvResumeState.EnvResumeStateKeys;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PipelineResumeUtilsTest extends WingsBaseTest {
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private PipelineService pipelineService;
  @Mock private AuthHandler authHandler;
  @Mock private FeatureFlagService featureFlagService;

  @Inject @InjectMocks private PipelineResumeUtils pipelineResumeUtils;

  private static String pipelineId = generateUuid();
  private static String pipelineResumeId = generateUuid();

  @Before
  public void setUp() {
    when(featureFlagService.isEnabled(eq(FeatureName.PIPELINE_RESUME), eq(ACCOUNT_ID))).thenReturn(true);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResume() {
    PipelineStage stage1 =
        PipelineStage.builder()
            .name("ps1")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(APPROVAL.name()).name("pse1").parallelIndex(1).build()))
            .build();
    PipelineStage stage2 =
        PipelineStage.builder()
            .name("ps2")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(ENV_STATE.name()).name("pse2").parallelIndex(2).build()))
            .build();
    PipelineStage stage3 =
        PipelineStage.builder()
            .name("ps3")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(ENV_STATE.name()).name("pse3").parallelIndex(3).build()))
            .build();
    PipelineStage stage4 =
        PipelineStage.builder()
            .name("ps4")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(ENV_STATE.name()).name("pse4").parallelIndex(4).build()))
            .build();

    String instanceId1 = generateUuid();
    String instanceId2 = generateUuid();
    String instanceId3 = generateUuid();
    StateExecutionInstance instance1 = mock(StateExecutionInstance.class);
    when(instance1.fetchStateExecutionData()).thenReturn(ApprovalStateExecutionData.builder().build());
    when(instance1.getUuid()).thenReturn(instanceId1);
    StateExecutionInstance instance2 = mock(StateExecutionInstance.class);
    when(instance2.fetchStateExecutionData()).thenReturn(anEnvStateExecutionData().build());
    when(instance2.getUuid()).thenReturn(instanceId2);
    StateExecutionInstance instance3 = mock(StateExecutionInstance.class);
    when(instance3.fetchStateExecutionData()).thenReturn(anEnvStateExecutionData().build());
    when(instance3.getUuid()).thenReturn(instanceId3);

    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        ImmutableMap.<String, StateExecutionInstance>builder()
            .put("pse1", instance1)
            .put("pse2", instance2)
            .put("pse3", instance3)
            .build();

    String wfId2 = generateUuid();
    String wfId3 = generateUuid();
    Pipeline pipeline = Pipeline.builder().pipelineStages(asList(stage1, stage2, stage3, stage4)).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    PipelineStageExecution stageExecution1 = PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build();
    PipelineStageExecution stageExecution2 =
        PipelineStageExecution.builder()
            .status(ExecutionStatus.SUCCESS)
            .workflowExecutions(Collections.singletonList(WorkflowExecution.builder().uuid(wfId2).build()))
            .build();
    PipelineStageExecution stageExecution3 =
        PipelineStageExecution.builder()
            .status(ExecutionStatus.FAILED)
            .workflowExecutions(Collections.singletonList(WorkflowExecution.builder().uuid(wfId3).build()))
            .build();
    PipelineStageExecution stageExecution4 = PipelineStageExecution.builder().build();
    workflowExecution.setPipelineExecution(
        aPipelineExecution()
            .withPipelineStageExecutions(asList(stageExecution1, stageExecution2, stageExecution3, stageExecution4))
            .build());

    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    Pipeline resumePipeline =
        pipelineResumeUtils.getPipelineForResume(APP_ID, 3, workflowExecution, stateExecutionInstanceMap);
    assertThat(resumePipeline).isNotNull();
    assertThat(resumePipeline.getPipelineStages()).isNotNull();
    assertThat(resumePipeline.getPipelineStages().size()).isEqualTo(4);

    PipelineStageElement pse = resumePipeline.getPipelineStages().get(0).getPipelineStageElements().get(0);
    assertThat(pse.getType()).isEqualTo(APPROVAL_RESUME.name());
    assertThat(pse.getProperties().size()).isEqualTo(2);
    assertThat(pse.getProperties().get(ApprovalResumeStateKeys.prevStateExecutionId)).isEqualTo(instanceId1);
    assertThat(pse.getProperties().get(ApprovalResumeStateKeys.prevPipelineExecutionId)).isEqualTo(pipelineId);

    pse = resumePipeline.getPipelineStages().get(1).getPipelineStageElements().get(0);
    assertThat(pse.getType()).isEqualTo(ENV_RESUME_STATE.name());
    assertThat(pse.getProperties().size()).isEqualTo(3);
    assertThat(pse.getProperties().get(EnvResumeStateKeys.prevStateExecutionId)).isEqualTo(instanceId2);
    assertThat(pse.getProperties().get(EnvResumeStateKeys.prevPipelineExecutionId)).isEqualTo(pipelineId);
    assertThat((List<String>) (pse.getProperties().get(EnvResumeStateKeys.prevWorkflowExecutionIds)))
        .containsExactly(wfId2);

    pse = resumePipeline.getPipelineStages().get(2).getPipelineStageElements().get(0);
    assertThat(pse.getType()).isEqualTo(ENV_STATE.name());

    pse = resumePipeline.getPipelineStages().get(3).getPipelineStageElements().get(0);
    assertThat(pse.getType()).isEqualTo(ENV_STATE.name());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResumeInsufficientStageExecutions() {
    PipelineStage stage1 =
        PipelineStage.builder()
            .name("ps1")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(APPROVAL.name()).name("pse1").parallelIndex(1).build()))
            .build();
    PipelineStage stage2 =
        PipelineStage.builder()
            .name("ps2")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(ENV_STATE.name()).name("pse2").parallelIndex(2).build()))
            .build();

    String instanceId1 = generateUuid();
    String instanceId2 = generateUuid();
    StateExecutionInstance instance1 = mock(StateExecutionInstance.class);
    when(instance1.fetchStateExecutionData()).thenReturn(ApprovalStateExecutionData.builder().build());
    when(instance1.getUuid()).thenReturn(instanceId1);
    StateExecutionInstance instance2 = mock(StateExecutionInstance.class);
    when(instance2.fetchStateExecutionData()).thenReturn(anEnvStateExecutionData().build());
    when(instance2.getUuid()).thenReturn(instanceId2);

    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        ImmutableMap.<String, StateExecutionInstance>builder().put("pse1", instance1).put("pse2", instance2).build();

    Pipeline pipeline = Pipeline.builder().pipelineStages(asList(stage1, stage2)).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    PipelineStageExecution stageExecution1 = PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build();
    workflowExecution.setPipelineExecution(
        aPipelineExecution().withPipelineStageExecutions(Collections.singletonList(stageExecution1)).build());

    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getPipelineForResume(APP_ID, 3, workflowExecution, stateExecutionInstanceMap);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResumeMismatchedTypes() {
    PipelineStage stage1 =
        PipelineStage.builder()
            .name("ps1")
            .pipelineStageElements(Collections.singletonList(
                PipelineStageElement.builder().type(APPROVAL.name()).name("pse1").parallelIndex(1).build()))
            .build();

    String instanceId1 = generateUuid();
    StateExecutionInstance instance1 = mock(StateExecutionInstance.class);
    when(instance1.fetchStateExecutionData()).thenReturn(anEnvStateExecutionData().build());
    when(instance1.getUuid()).thenReturn(instanceId1);

    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        ImmutableMap.<String, StateExecutionInstance>builder().put("pse1", instance1).build();

    Pipeline pipeline = Pipeline.builder().pipelineStages(Collections.singletonList(stage1)).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    PipelineStageExecution stageExecution1 = PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build();
    workflowExecution.setPipelineExecution(
        aPipelineExecution().withPipelineStageExecutions(Collections.singletonList(stageExecution1)).build());

    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getPipelineForResume(APP_ID, 3, workflowExecution, stateExecutionInstanceMap);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResumeEmptyPipeline() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(null);
    pipelineResumeUtils.getPipelineForResume(APP_ID, 0, workflowExecution, null);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResumeEmptyPipelineStages() {
    Pipeline pipeline = Pipeline.builder().build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getPipelineForResume(APP_ID, 0, workflowExecution, null);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetPipelineForResumeEmptyPipelineStageExecutions() {
    Pipeline pipeline =
        Pipeline.builder().pipelineStages(Collections.singletonList(PipelineStage.builder().build())).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    workflowExecution.setPipelineExecution(aPipelineExecution().build());
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getPipelineForResume(APP_ID, 0, workflowExecution, null);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdatePipelineExecutionsAfterResume() {
    Query<WorkflowExecution> query = mock(Query.class);
    when(mockWingsPersistence.createQuery(eq(WorkflowExecution.class))).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    UpdateOperations<WorkflowExecution> updateOperations = mock(UpdateOperations.class);
    when(mockWingsPersistence.createUpdateOperations(eq(WorkflowExecution.class))).thenReturn(updateOperations);

    WorkflowExecution currWorkflowExecution = prepareWorkflowExecution();
    WorkflowExecution prevWorkflowExecution = prepareWorkflowExecution();
    verifyUpdatePipelineExecutionsAfterResume(pipelineId, currWorkflowExecution, prevWorkflowExecution);
    verify(mockWingsPersistence, times(2)).update(any(Query.class), any(UpdateOperations.class));

    currWorkflowExecution = prepareWorkflowExecution();
    prevWorkflowExecution = prepareLatestResumedWorkflowExecution();
    verifyUpdatePipelineExecutionsAfterResume(pipelineResumeId, currWorkflowExecution, prevWorkflowExecution);
    verify(mockWingsPersistence, times(4)).update(any(Query.class), any(UpdateOperations.class));
  }

  private void verifyUpdatePipelineExecutionsAfterResume(
      String pipelineResumeId, WorkflowExecution currWorkflowExecution, WorkflowExecution prevWorkflowExecution) {
    pipelineResumeUtils.updatePipelineExecutionsAfterResume(currWorkflowExecution, prevWorkflowExecution);
    assertThat(currWorkflowExecution.getPipelineResumeId()).isEqualTo(pipelineResumeId);
    assertThat(currWorkflowExecution.isLatestPipelineResume()).isTrue();
    assertThat(prevWorkflowExecution.getPipelineResumeId()).isEqualTo(pipelineResumeId);
    assertThat(prevWorkflowExecution.isLatestPipelineResume()).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeStages() {
    PipelineStage stage1 = PipelineStage.builder().name("ps1").build();
    PipelineStage stage2 = PipelineStage.builder().name("ps2").build();
    PipelineStage stage3 = PipelineStage.builder().name("ps3").build();
    Pipeline pipeline = Pipeline.builder().pipelineStages(asList(stage1, stage2, stage3)).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    PipelineStageExecution stageExecution1 = PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build();
    PipelineStageExecution stageExecution2 = PipelineStageExecution.builder().status(ExecutionStatus.FAILED).build();
    PipelineStageExecution stageExecution3 = PipelineStageExecution.builder().build();
    workflowExecution.setPipelineExecution(
        aPipelineExecution()
            .withPipelineStageExecutions(asList(stageExecution1, stageExecution2, stageExecution3))
            .build());
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    List<PipelineStage> stages = pipelineResumeUtils.getResumeStages(APP_ID, workflowExecution);
    assertThat(stages).isNotNull();
    assertThat(stages.size()).isEqualTo(2);
    assertThat(stages.stream().map(PipelineStage::getName).collect(Collectors.toList())).containsExactly("ps1", "ps2");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeStagesInsufficientStageExecutions() {
    PipelineStage stage1 = PipelineStage.builder().name("ps1").build();
    PipelineStage stage2 = PipelineStage.builder().name("ps2").build();
    PipelineStage stage3 = PipelineStage.builder().name("ps3").build();
    Pipeline pipeline = Pipeline.builder().pipelineStages(asList(stage1, stage2, stage3)).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    PipelineStageExecution stageExecution1 = PipelineStageExecution.builder().status(ExecutionStatus.SUCCESS).build();
    workflowExecution.setPipelineExecution(
        aPipelineExecution().withPipelineStageExecutions(Collections.singletonList(stageExecution1)).build());
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getResumeStages(APP_ID, workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeStagesEmptyPipeline() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(null);
    pipelineResumeUtils.getResumeStages(APP_ID, workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeStagesEmptyPipelineStages() {
    Pipeline pipeline = Pipeline.builder().build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getResumeStages(APP_ID, workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeStagesEmptyPipelineStageExecutions() {
    Pipeline pipeline =
        Pipeline.builder().pipelineStages(Collections.singletonList(PipelineStage.builder().build())).build();
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    workflowExecution.setPipelineExecution(aPipelineExecution().build());
    when(pipelineService.readPipelineWithResolvedVariables(any(), any(), any())).thenReturn(pipeline);
    pipelineResumeUtils.getResumeStages(APP_ID, workflowExecution);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetResumeHistory() {
    Query<WorkflowExecution> query = mock(Query.class);
    when(mockWingsPersistence.createQuery(eq(WorkflowExecution.class))).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.project(any(), anyBoolean())).thenReturn(query);
    when(query.order(anyString())).thenReturn(query);
    when(query.asList())
        .thenReturn(asList(prepareWorkflowExecution(), prepareNonLatestResumedWorkflowExecution(),
            prepareLatestResumedWorkflowExecution()));

    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    List<WorkflowExecution> history = pipelineResumeUtils.getResumeHistory(APP_ID, workflowExecution);
    assertThat(history).isNotNull();
    assertThat(history).isEmpty();

    workflowExecution = prepareNonLatestResumedWorkflowExecution();
    history = pipelineResumeUtils.getResumeHistory(APP_ID, workflowExecution);
    assertThat(history).isNotNull();
    assertThat(history.size()).isEqualTo(3);

    workflowExecution = prepareLatestResumedWorkflowExecution();
    history = pipelineResumeUtils.getResumeHistory(APP_ID, workflowExecution);
    assertThat(history).isNotNull();
    assertThat(history.size()).isEqualTo(3);
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAuthorizeTriggerPipelineResume() {
    authorizeExecuteDeployments();
    pipelineResumeUtils.authorizeTriggerPipelineResume(APP_ID, prepareWorkflowExecution());
  }

  @Test(expected = Exception.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAuthorizeTriggerPipelineResumeFailure() {
    authorizeReadDeployments();
    pipelineResumeUtils.authorizeTriggerPipelineResume(APP_ID, prepareWorkflowExecution());
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAuthorizeReadPipelineResume() {
    authorizeReadDeployments();
    pipelineResumeUtils.authorizeReadPipelineResume(APP_ID, prepareWorkflowExecution());
  }

  @Test(expected = Exception.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAuthorizeReadPipelineResumeFailure() {
    authorizeExecuteDeployments();
    pipelineResumeUtils.authorizeReadPipelineResume(APP_ID, prepareWorkflowExecution());
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeAvailable() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);

    workflowExecution = prepareLatestResumedWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeAvailableForFFOff() {
    when(featureFlagService.isEnabled(eq(FeatureName.PIPELINE_RESUME), eq(ACCOUNT_ID))).thenReturn(false);
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeAvailableForSuccessfulExecution() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeAvailableForNonLatestExecution() {
    WorkflowExecution workflowExecution = prepareNonLatestResumedWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeAvailableForTypeWorkflow() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution(WorkflowType.ORCHESTRATION);
    pipelineResumeUtils.checkPipelineResumeAvailable(workflowExecution);
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeHistoryAvailable() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeHistoryAvailable(workflowExecution);

    workflowExecution = prepareLatestResumedWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeHistoryAvailable(workflowExecution);

    workflowExecution = prepareNonLatestResumedWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeHistoryAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeHistoryAvailableForFFOff() {
    when(featureFlagService.isEnabled(eq(FeatureName.PIPELINE_RESUME), eq(ACCOUNT_ID))).thenReturn(false);
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    pipelineResumeUtils.checkPipelineResumeHistoryAvailable(workflowExecution);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCheckPipelineResumeHistoryAvailableForTypeWorkflow() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution(WorkflowType.ORCHESTRATION);
    pipelineResumeUtils.checkPipelineResumeHistoryAvailable(workflowExecution);
  }

  private void authorizeReadDeployments() {
    authorizeDeployments(Action.READ);
  }

  private void authorizeExecuteDeployments() {
    authorizeDeployments(Action.EXECUTE);
  }

  private void authorizeDeployments(Action action) {
    when(authHandler.authorize(any(), any(), any())).thenAnswer((Answer<Boolean>) invocation -> {
      List<PermissionAttribute> permissionAttributes = invocation.getArgumentAt(0, List.class);
      if (isEmpty(permissionAttributes)) {
        return Boolean.TRUE;
      }

      if (permissionAttributes.stream().anyMatch(
              pa -> pa.getPermissionType() != PermissionType.DEPLOYMENT || pa.getAction() != action)) {
        throw new Exception();
      }
      return Boolean.TRUE;
    });
  }

  private WorkflowExecution prepareWorkflowExecution(WorkflowType workflowType) {
    return WorkflowExecution.builder()
        .uuid(pipelineId)
        .accountId(ACCOUNT_ID)
        .workflowType(workflowType)
        .status(ExecutionStatus.FAILED)
        .executionArgs(new ExecutionArgs())
        .build();
  }

  private WorkflowExecution prepareWorkflowExecution() {
    return prepareWorkflowExecution(WorkflowType.PIPELINE);
  }

  private WorkflowExecution prepareLatestResumedWorkflowExecution() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    workflowExecution.setPipelineResumeId(pipelineResumeId);
    workflowExecution.setLatestPipelineResume(true);
    return workflowExecution;
  }

  private WorkflowExecution prepareNonLatestResumedWorkflowExecution() {
    WorkflowExecution workflowExecution = prepareWorkflowExecution();
    workflowExecution.setPipelineResumeId(pipelineResumeId);
    workflowExecution.setLatestPipelineResume(false);
    return workflowExecution;
  }
}
