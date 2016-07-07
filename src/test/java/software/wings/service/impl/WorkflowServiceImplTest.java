/**
 *
 */

package software.wings.service.impl;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.INFRA_ID;

import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Host;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineTest;
import software.wings.sm.StateMachineTest.StateAsync;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.StateType;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.sm.states.ForkState;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javax.inject.Inject;

/**
 * The type Workflow service impl test.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceImplTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  private static Map<String, CountDownLatch> workflowExecutionSignals = new HashMap<>();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock @Inject private StaticConfiguration staticConfiguration;
  @Inject private ServiceInstanceService serviceInstanceService;

  /**
   * Should trigger.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTrigger() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);

    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, smId, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000), true);
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    StateSync stateD = new StateSync("stateD" + new Random().nextInt(10000));
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);

    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, smId, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + new Random().nextInt(10000), true);
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    StateMachineTest.StateSync stateD = new StateMachineTest.StateSync("stateD" + new Random().nextInt(10000));
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);

    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, smId, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, sm.getUuid(), executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + new Random().nextInt(10000), 2000, true);
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, sm.getUuid(), executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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
   * Should trigger and fail asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerAndFailAsync() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + new Random().nextInt(10000), 2000, true);
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, sm.getUuid(), executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

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
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + new Random().nextInt(10000), 2000, false, true);
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, sm.getUuid(), executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateA executed before StateC")
        .isEqualTo(true);
  }

  private StateMachine createAsyncSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB", 5000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC", 2000);
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

    sm = svc.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();
    return sm;
  }

  /**
   * Should trigger simple fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerSimpleFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, sm.getUuid(), executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();
  }

  /**
   * Should trigger mixed fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerMixedFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB", 3000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC", 1000);
    sm.addState(stateBC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, sm.getUuid(), executionUuid);

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowServiceImpl) workflowService).trigger(appId, smId, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();
  }

  /**
   * Should read simple workflow.
   */
  @Test
  public void shouldReadSimpleWorkflow() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    Orchestration workflow = impl.readLatestSimpleWorkflow(app.getUuid(), env.getUuid());
    assertThat(workflow).isNotNull();
    assertThat(workflow.getWorkflowType()).isEqualTo(WorkflowType.SIMPLE);
    assertThat(workflow.getGraph()).isNotNull();
    assertThat(workflow.getGraph().getNodes()).isNotNull();
    assertThat(workflow.getGraph().getNodes().size()).isEqualTo(3);
    assertThat(workflow.getGraph().getLinks()).isNotNull();
    assertThat(workflow.getGraph().getLinks().size()).isEqualTo(2);
  }

  /**
   * Should trigger simple workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode().withId("n0").withName("ORIGIN").withX(200).withY(50).build())
            .addNodes(aNode()
                          .withId("n1")
                          .withName("RepeatByInstances")
                          .withX(200)
                          .withY(50)
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                          .build())
            .addNodes(
                aNode()
                    .withId("n2")
                    .withName("email")
                    .withX(250)
                    .withY(50)
                    .withType(StateType.EMAIL.name())
                    .addProperty("toAddress", "a@b.com")
                    .addProperty("subject", "commandName : ${SIMPLE_WORKFLOW_COMMAND_NAME}")
                    .addProperty("body",
                        "service:${service.name}, serviceTemplate:${serviceTemplate.name}, host:${host.name}, instance:${instance.name}")
                    .build())
            .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
            .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withInfraId(INFRA_ID).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withInfraId(INFRA_ID).withHostName("host2").build());
    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withService(service)
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    String uuid1 = serviceInstanceService.save(builder.withHost(host1).build()).getUuid();
    String uuid2 = serviceInstanceService.save(builder.withHost(host2).build()).getUuid();

    ExecutionArgs executionArgs = new ExecutionArgs();
    List<String> serviceInstanceIds = new ArrayList<>();
    serviceInstanceIds.add(uuid1);
    serviceInstanceIds.add(uuid2);
    executionArgs.setServiceInstanceIds(serviceInstanceIds);
    executionArgs.setExecutionStrategy(ExecutionStrategy.SERIAL);
    executionArgs.setCommandName("START");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId("123");

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;

    impl.setStaticConfiguration(staticConfiguration);

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution workflowExecution =
        impl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(workflowExecution2.getGraph()).isNotNull();
  }

  /**
   * Trigger pipeline.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void triggerPipeline() throws InterruptedException {
    Pipeline pipeline = createPipeline();
    triggerPipeline(pipeline);
  }

  private WorkflowExecution triggerPipeline(Pipeline pipeline) throws InterruptedException {
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution =
        ((WorkflowServiceImpl) workflowService).triggerPipelineExecution(appId, pipeline.getUuid(), callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    return execution;
  }

  /**
   * Should update pipeline with graph.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldListPipelineExecutions() throws InterruptedException {
    Pipeline pipeline = createPipeline();
    WorkflowExecution workflowExecution = triggerPipeline(pipeline);
    PageRequest<WorkflowExecution> pageRequest = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowType");
    filter.setFieldValues(WorkflowType.PIPELINE);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    PageResponse<WorkflowExecution> pageResponse = workflowService.listExecutions(pageRequest, true);
    assertThat(pageResponse).isNotNull().hasSize(1).doesNotContainNull();
    WorkflowExecution workflowExecution2 = pageResponse.get(0);
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), appId, workflowExecution.getStateMachineId(), pipeline.getUuid());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(workflowExecution2.getGraph()).isNotNull();
  }

  private Pipeline createPipeline() {
    Graph graph = createInitialGraph();
    Pipeline pipeline = aPipeline()
                            .withAppId(appId)
                            .withName("pipeline1")
                            .withDescription("Sample Pipeline")
                            .addServices("service1", "service2")
                            .withGraph(graph)
                            .build();

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting(StateMachine::getGraph)
        .doesNotContainNull()
        .containsExactly(graph);
    return pipeline;
  }

  /**
   * @return
   */
  private Graph createInitialGraph() {
    return aGraph()
        .addNodes(aNode().withId("n0").withName("ORIGIN").withX(200).withY(50).withType(StateType.BUILD.name()).build())
        .addNodes(aNode().withId("n1").withName("BUILD").withX(200).withY(50).withType(StateType.BUILD.name()).build())
        .addNodes(aNode()
                      .withId("n2")
                      .withName("IT")
                      .withX(250)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "12345")
                      .build())
        .addNodes(aNode()
                      .withId("n3")
                      .withName("QA")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "23456")
                      .build())
        .addNodes(aNode()
                      .withId("n4")
                      .withName("UAT")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "34567")
                      .build())
        .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build())
        .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
        .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
        .addLinks(aLink().withId("l3").withFrom("n3").withTo("n4").withType("success").build())
        .build();
  }

  /**
   * Should trigger orchestration.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerOrchestration() throws InterruptedException {
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());
    triggerOrchestration(env);
  }

  /**
   * Trigger orchestration.
   *
   * @param env the env
   * @throws InterruptedException the interrupted exception
   */
  public void triggerOrchestration(Environment env) throws InterruptedException {
    Orchestration orchestration = createExecutableOrchestration(env);
    ExecutionArgs executionArgs = new ExecutionArgs();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution =
        ((WorkflowServiceImpl) workflowService)
            .triggerOrchestrationExecution(appId, env.getUuid(), orchestration.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
  }

  private Orchestration createExecutableOrchestration(Environment env) {
    Graph graph =
        aGraph()
            .addNodes(
                aNode().withId("n0").withName("ORIGIN").withX(200).withY(50).withType(StateType.BUILD.name()).build())
            .addNodes(aNode()
                          .withId("n1")
                          .withName("wait")
                          .withX(200)
                          .withY(50)
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1l)
                          .build())
            .addNodes(aNode()
                          .withId("n2")
                          .withName("email")
                          .withX(250)
                          .withY(50)
                          .withType(StateType.EMAIL.name())
                          .addProperty("toAddress", "a@b.com")
                          .addProperty("subject", "testing")
                          .build())
            .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(appId)
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withEnvironment(env)
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    return orchestration;
  }

  /**
   * Should list orchestration.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldListOrchestration() throws InterruptedException {
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());

    triggerOrchestration(env);

    // 2nd orchestration
    Orchestration orchestration = createExecutableOrchestration(env);
    PageRequest<Orchestration> pageRequest = new PageRequest<>();
    PageResponse<Orchestration> res = workflowService.listOrchestration(pageRequest);

    assertThat(res).isNotNull().hasSize(2);
  }

  /**
   * The type Workflow execution update mock.
   */
  public static class WorkflowExecutionUpdateMock extends WorkflowExecutionUpdate {
    private String signalId;

    /**
     * Instantiates a new Workflow execution update mock.
     */
    public WorkflowExecutionUpdateMock() {}

    /**
     * Instantiates a new Workflow execution update mock.
     *
     * @param signalId the signal id
     */
    public WorkflowExecutionUpdateMock(String signalId) {
      super();
      this.signalId = signalId;
    }

    @Override
    public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
      System.out.println(status);
      super.callback(context, status, ex);
      workflowExecutionSignals.get(signalId).countDown();
    }

    /**
     * Gets signal id.
     *
     * @return the signal id
     */
    public String getSignalId() {
      return signalId;
    }

    /**
     * Sets signal id.
     *
     * @param signalId the signal id
     */
    public void setSignalId(String signalId) {
      this.signalId = signalId;
    }
  }
}
