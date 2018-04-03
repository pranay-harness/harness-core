package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachineTest.StateAsync;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.states.ForkState;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by rishi on 2/25/17.
 */
@Listeners(NotifyEventListener.class)
public class StateMachineExecutorTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutorTest.class);

  @Inject WingsPersistence wingsPersistence;
  @Inject StateMachineExecutor stateMachineExecutor;

  @Inject private WorkflowService workflowService;

  /**
   * Should trigger.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTrigger() throws InterruptedException {
    String appId = generateUuid();

    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();

    stateMachineExecutor.execute(appId, smId, executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateA executed before StateB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateB executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should trigger failed transition.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerFailedTransition() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique(), true);
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);
    StateSync stateD = new StateSync("stateD" + StaticMap.getUnique());
    sm.addState(stateD);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateD)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();

    stateMachineExecutor.execute(appId, smId, executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat(StaticMap.getValue(stateD.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateA executed before StateB")
        .isEqualTo(true);
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateD.getName()))
        .as("StateB executed before StateD")
        .isEqualTo(true);
  }

  /**
   * Should trigger and fail.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerAndFail() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + StaticMap.getUnique(), true);
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);
    StateMachineTest.StateSync stateD = new StateMachineTest.StateSync("stateD" + StaticMap.getUnique());
    sm.addState(stateD);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateC)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateD)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();

    stateMachineExecutor.execute(appId, smId, executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat(StaticMap.getValue(stateD.getName())).isNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateA executed before StateB")
        .isEqualTo(true);
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat(StaticMap.getValue(stateD.getName())).isNull();
  }

  /**
   * Should trigger asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerAsync() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + nextInt(0, 10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + nextInt(0, 10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + nextInt(0, 10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + nextInt(0, 10000), 2000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC" + nextInt(0, 10000), 1000);
    sm.addState(stateBC);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateBC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateBC)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateAB executed before StateB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateBC.getName()))
        .as("StateB executed before StateBC")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateBC.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateBC executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should trigger failed asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerFailedAsync() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + StaticMap.getUnique(), 600, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateAB executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should mark success .
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldAdviceToMarkSuccess() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + StaticMap.getUnique(), 100, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    CustomeExecutionEventAdvisor advisor = new CustomeExecutionEventAdvisor(ExecutionInterruptType.MARK_SUCCESS);
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback), advisor);
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateAB executed before StateB")
        .isEqualTo(true);
  }

  /**
   * Should mark failed
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldAdviceToMarkFailed() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + StaticMap.getUnique(), 100, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    CustomeExecutionEventAdvisor advisor = new CustomeExecutionEventAdvisor(ExecutionInterruptType.MARK_FAILED);
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback), advisor);
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateAB executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should mark aborted
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldAdviceToMarkAborted() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + StaticMap.getUnique(), 100, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    CustomeExecutionEventAdvisor advisor = new CustomeExecutionEventAdvisor(ExecutionInterruptType.ABORT);
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback), advisor);
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
  }

  public static class CustomeExecutionEventAdvisor implements ExecutionEventAdvisor {
    private ExecutionInterruptType executionInterruptType;

    public CustomeExecutionEventAdvisor() {}

    public CustomeExecutionEventAdvisor(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
    }

    @Override
    public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
      if (executionEvent.getExecutionStatus() == ExecutionStatus.FAILED) {
        return anExecutionEventAdvice().withExecutionInterruptType(executionInterruptType).build();
      }
      return null;
    }

    public ExecutionInterruptType getExecutionInterruptType() {
      return executionInterruptType;
    }

    public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
    }
  }

  /**
   * Should trigger and fail asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerAndFailAsync() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + StaticMap.getUnique(), 500, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
  }

  /**
   * Should fail after exception
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldFailAfterException() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + StaticMap.getUnique(), 500, false, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback));
    callback.await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateA executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should trigger simple fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerSimpleFork() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<>();
    forkStates.add(stateB.getName());
    forkStates.add(stateC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(fork1)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, sm.getUuid(), executionUuid, executionUuid, null, asList(callback));
    callback.await();
  }

  /**
   * Should trigger mixed fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerMixedFork() throws InterruptedException {
    String appId = generateUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB", 1000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC", 100);
    sm.addState(stateBC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<>();
    forkStates.add(stateB.getName());
    forkStates.add(stateBC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(fork1)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(fork1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    logger.info("Going to trigger state machine");
    String executionUuid = generateUuid();

    StateMachineExecutionCallbackMock callback = new StateMachineExecutionCallbackMock();
    stateMachineExecutor.execute(appId, smId, executionUuid, executionUuid, null, asList(callback));
    callback.await();
  }

  @Test
  public void shouldCleanForRetry() {
    List<ContextElement> originalNotifyElements = asList(anInstanceElement().withDisplayName("foo").build());

    String prevStateExecutionInstanceId = wingsPersistence.save(aStateExecutionInstance()
                                                                    .withAppId("appId")
                                                                    .withStateName("state0")
                                                                    .withNotifyElements(originalNotifyElements)
                                                                    .build());

    HashMap<String, StateExecutionData> stateExecutionMap = new HashMap<>();
    stateExecutionMap.put("state0", new StateExecutionData());
    stateExecutionMap.put("state1", new StateExecutionData());

    List<ContextElement> notifyElements =
        asList(anInstanceElement().withDisplayName("bar").build(), originalNotifyElements.get(0));

    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withAppId("appId")
                                                        .withStateName("state1")
                                                        .withStateExecutionMap(stateExecutionMap)
                                                        .withPrevInstanceId(prevStateExecutionInstanceId)
                                                        .withStatus(FAILED)
                                                        .build();

    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
    stateMachineExecutor.clearStateExecutionData(stateExecutionInstance, null);

    stateExecutionInstance = wingsPersistence.get(StateExecutionInstance.class, stateExecutionInstance.getUuid());

    // TODO: add more checks
    assertThat(stateExecutionInstance.getStatus()).isEqualTo(NEW);
    assertThat(stateExecutionInstance.getNotifyElements()).isEqualTo(originalNotifyElements);
  }
}
