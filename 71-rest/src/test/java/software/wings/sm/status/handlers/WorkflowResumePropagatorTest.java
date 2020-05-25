package software.wings.sm.status.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.sm.status.StateStatusUpdateInfo;

public class WorkflowResumePropagatorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  public static final String APP_ID = generateUuid();
  public static final String WORKFLOW_EXECUTION_ID = generateUuid();
  public static final String PIPELINE_EXECUTION_ID = generateUuid();
  public static final String STATE_EXECUTION_ID = generateUuid();

  @Inject private WorkflowResumePropagator resumePropagator;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleStatusUpdateForWorkflowRunning() {
    buildAndSave(ExecutionStatus.RUNNING);
    StateStatusUpdateInfo updateInfo = StateStatusUpdateInfo.builder()
                                           .appId(APP_ID)
                                           .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                           .stateExecutionInstanceId(STATE_EXECUTION_ID)
                                           .status(ExecutionStatus.PAUSED)
                                           .build();
    resumePropagator.handleStatusUpdate(updateInfo);
    WorkflowExecution execution = fetchExecution(APP_ID, WORKFLOW_EXECUTION_ID);
    assertThat(execution).isNotNull();
    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);

    WorkflowExecution pipelineExecution = fetchExecution(APP_ID, PIPELINE_EXECUTION_ID);
    assertThat(pipelineExecution).isNotNull();
    assertThat(pipelineExecution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleStatusUpdateForWorkflowPaused() {
    buildAndSave(ExecutionStatus.PAUSED);
    StateStatusUpdateInfo updateInfo = StateStatusUpdateInfo.builder()
                                           .appId(APP_ID)
                                           .workflowExecutionId(WORKFLOW_EXECUTION_ID)
                                           .stateExecutionInstanceId(STATE_EXECUTION_ID)
                                           .status(ExecutionStatus.PAUSED)
                                           .build();
    resumePropagator.handleStatusUpdate(updateInfo);
    WorkflowExecution execution = fetchExecution(APP_ID, WORKFLOW_EXECUTION_ID);
    assertThat(execution).isNotNull();
    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);

    WorkflowExecution pipelineExecution = fetchExecution(APP_ID, PIPELINE_EXECUTION_ID);
    assertThat(pipelineExecution).isNotNull();
    assertThat(pipelineExecution.getStatus()).isEqualTo(ExecutionStatus.PAUSED);
  }

  private WorkflowExecution fetchExecution(String appId, String workflowExecutionId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter(WorkflowExecutionKeys.appId, appId)
        .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
        .get();
  }

  private void buildAndSave(ExecutionStatus internalStatus) {
    wingsPersistence.save(
        WorkflowExecution.builder()
            .uuid(PIPELINE_EXECUTION_ID)
            .appId(APP_ID)
            .workflowId(generateUuid())
            .workflowType(WorkflowType.PIPELINE)
            .status(ExecutionStatus.PAUSED)
            .pipelineExecution(aPipelineExecution()
                                   .withPipelineStageExecutions(
                                       singletonList(PipelineStageExecution.builder().status(internalStatus).build()))
                                   .build())
            .build());
    wingsPersistence.save(WorkflowExecution.builder()
                              .uuid(WORKFLOW_EXECUTION_ID)
                              .appId(APP_ID)
                              .workflowId(generateUuid())
                              .workflowType(WorkflowType.ORCHESTRATION)
                              .pipelineExecutionId(PIPELINE_EXECUTION_ID)
                              .status(ExecutionStatus.PAUSED)
                              .build());
  }
}