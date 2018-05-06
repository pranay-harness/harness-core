package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.STATE_MACHINE_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.states.BarrierState;
import software.wings.waitnotify.NotifyEventListener;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowExecutionServiceDBTest extends WingsBaseTest {
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;

  @Before
  public void init() {
    wingsPersistence.save(aStateMachine()
                              .withAppId(APP_ID)
                              .withUuid(STATE_MACHINE_ID)
                              .withInitialStateName("foo")
                              .addState(new BarrierState("foo"))
                              .build());
  }

  @Test
  public void shouldListExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    final WorkflowExecutionBuilder workflowExecutionBuilder =
        aWorkflowExecution().withAppId(APP_ID).withEnvId(ENV_ID).withStateMachineId(STATE_MACHINE_ID);

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(SUCCESS).build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid())
                              .withStatus(ExecutionStatus.ERROR)
                              .withBreakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid())
                              .withStatus(ExecutionStatus.FAILED)
                              .withBreakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(
        workflowExecutionBuilder.withUuid(generateUuid()).withStatus(ExecutionStatus.ABORTED).build());

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(RUNNING).build());

    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(PAUSED).build());
    wingsPersistence.save(workflowExecutionBuilder.withUuid(generateUuid()).withStatus(WAITING).build());

    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(
        aPageRequest().addFilter(WorkflowExecution.APP_ID_KEY, Operator.EQ, APP_ID).build(), false, true, false, true);
    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.size()).isEqualTo(7);
  }
}