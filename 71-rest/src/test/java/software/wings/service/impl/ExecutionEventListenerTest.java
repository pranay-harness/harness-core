package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateMachineExecutor;

/**
 * Created by sgurubelli on 7/30/18.
 */
public class ExecutionEventListenerTest extends WingsBaseTest {
  @Inject @InjectMocks private ExecutionEventListener executionEventListener;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private StateMachineExecutor stateMachineExecutor;

  @Test
  public void shouldNoQueueIfNotRunningOrPaused() throws Exception {
    wingsPersistence.save(WorkflowExecution.builder().appId(APP_ID).workflowId(WORKFLOW_ID).status(SUCCESS).build());

    executionEventListener.onMessage(ExecutionEvent.builder().appId(APP_ID).workflowId(WORKFLOW_ID).build());

    verify(stateMachineExecutor, times(0)).startQueuedExecution(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldNoQueueIfNotQueued() throws Exception {
    wingsPersistence.save(WorkflowExecution.builder().appId(APP_ID).workflowId(WORKFLOW_ID).status(RUNNING).build());

    executionEventListener.onMessage(ExecutionEvent.builder().appId(APP_ID).workflowId(WORKFLOW_ID).build());

    verify(stateMachineExecutor, times(0)).startQueuedExecution(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldQueueBuildWorkflow() throws Exception {
    WorkflowExecution queuedExecution =
        WorkflowExecution.builder().appId(APP_ID).workflowId(WORKFLOW_ID).status(QUEUED).build();

    wingsPersistence.save(queuedExecution);

    when(stateMachineExecutor.startQueuedExecution(APP_ID, queuedExecution.getUuid())).thenReturn(true);
    executionEventListener.onMessage(ExecutionEvent.builder().appId(APP_ID).workflowId(WORKFLOW_ID).build());

    verify(stateMachineExecutor).startQueuedExecution(APP_ID, queuedExecution.getUuid());
  }

  @Test
  public void shouldQueueWorkflow() throws Exception {
    WorkflowExecution queuedExecution = WorkflowExecution.builder()
                                            .infraMappingIds(asList(INFRA_MAPPING_ID))
                                            .appId(APP_ID)
                                            .workflowId(WORKFLOW_ID)
                                            .status(QUEUED)
                                            .build();

    wingsPersistence.save(queuedExecution);

    executionEventListener.onMessage(ExecutionEvent.builder()
                                         .infraMappingIds(asList(INFRA_MAPPING_ID))
                                         .appId(APP_ID)
                                         .workflowId(WORKFLOW_ID)
                                         .build());

    verify(stateMachineExecutor).startQueuedExecution(APP_ID, queuedExecution.getUuid());
  }
}
