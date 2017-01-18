/**
 *
 */

package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.DeploymentType.SSH;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.OrchestrationWorkflow.OrchestrationWorkflowBuilder.anOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ErrorCodes;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Orchestration;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowOrchestrationType;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventType;
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
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * The type Workflow service impl test.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowExecutionServiceImplTest extends WingsBaseTest {
  private static Map<String, CountDownLatch> workflowExecutionSignals = new HashMap<>();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowExecutionService workflowExecutionService;
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
    String appId = UUIDGenerator.getUuid();

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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, smId, executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, smId, executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, smId, executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, sm.getUuid(), executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB" + new Random().nextInt(10000), 600, true);
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, sm.getUuid(), executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + new Random().nextInt(10000), 500, true);
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, sm.getUuid(), executionUuid, executionUuid, callback);
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
    String appId = UUIDGenerator.getUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSync stateB = new StateMachineTest.StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSync stateC = new StateMachineTest.StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsync("StateAB" + new Random().nextInt(10000), 500, false, true);
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, sm.getUuid(), executionUuid, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateA executed before StateC")
        .isEqualTo(true);
  }

  private StateMachine createAsyncSM(WorkflowService svc, String appId) {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB", 2000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC", 500);
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
    String appId = UUIDGenerator.getUuid();
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
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, sm.getUuid(), executionUuid, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();
  }

  /**
   * Should trigger mixed fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerMixedFork() throws InterruptedException {
    String appId = UUIDGenerator.getUuid();
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateAsync("StateAB", 1000);
    sm.addState(stateAB);
    State stateBC = new StateAsync("StateBC", 100);
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
    workflowExecutionService.trigger(appId, sm.getUuid(), executionUuid, executionUuid);

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    ((WorkflowExecutionServiceImpl) workflowExecutionService)
        .trigger(appId, smId, executionUuid, executionUuid, callback);
    workflowExecutionSignals.get(signalId).await();
  }

  /**
   * Should read simple workflow.
   */
  @Test
  public void shouldReadSimpleWorkflow() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Orchestration workflow = workflowService.readLatestSimpleWorkflow(app.getUuid());
    assertThat(workflow).isNotNull();
    assertThat(workflow.getWorkflowType()).isEqualTo(WorkflowType.SIMPLE);
    assertThat(workflow.getGraph()).isNotNull();
    assertThat(workflow.getGraph().getNodes()).isNotNull();
    assertThat(workflow.getGraph().getNodes().size()).isEqualTo(2);
    assertThat(workflow.getGraph().getLinks()).isNotNull();
    assertThat(workflow.getGraph().getLinks().size()).isEqualTo(1);
  }

  /**
   * Should trigger simple workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerSimpleWorkflow() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("n1")
                          .withOrigin(true)
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
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
            .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));
    executionArgs.setExecutionStrategy(ExecutionStrategy.SERIAL);
    executionArgs.setCommandName("START");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId(service.getUuid());

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    impl.setStaticConfiguration(staticConfiguration);

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    assertThat(workflowExecution2.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByInstances")
        .hasFieldOrPropertyWithValue("type", "REPEAT")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(workflowExecution2.getExecutionNode().getGroup()).isNotNull();
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("host1:TEMPLATE_NAME", "host2:TEMPLATE_NAME");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("type")
        .contains("ELEMENT", "ELEMENT");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("email", "email");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("type")
        .contains("EMAIL", "EMAIL");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("SUCCESS", "SUCCESS");

    assertThat(workflowExecution2.getExecutionNode())
        .hasFieldOrProperty("elementStatusSummary")
        .hasFieldOrProperty("instanceStatusSummary");
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary()).isNotNull().hasSize(2);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .isNotNull()
        .extracting("instancesCount", "status")
        .containsExactly(1, ExecutionStatus.SUCCESS);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary()).isNotNull().hasSize(2);
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .isNotNull()
        .extracting("startTs", "endTs")
        .doesNotContainNull();
    assertThat(workflowExecution2.getExecutionNode().getElementStatusSummary().get(0))
        .extracting("contextElement")
        .doesNotContainNull()
        .extracting("elementType")
        .hasSize(1)
        .containsExactly(ContextElementType.INSTANCE);
  }

  /**
   * Should trigger simple workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldRenderSimpleWorkflow() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("n1")
                                    .withOrigin(true)
                                    .withName("RepeatByInstances")
                                    .withX(200)
                                    .withY(50)
                                    .withType(StateType.REPEAT.name())
                                    .addProperty("repeatElementExpression", "${instances()}")
                                    .addProperty("executionStrategyExpression", "${SIMPLE_WORKFLOW_REPEAT_STRATEGY}")
                                    .build())
                      .addNodes(aNode()
                                    .withId("n2")
                                    .withName("stop")
                                    .withX(250)
                                    .withY(50)
                                    .withType(StateType.COMMAND.name())
                                    .addProperty("commandName", "${SIMPLE_WORKFLOW_COMMAND_NAME}")
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("repeat").build())
                      .build();

    when(staticConfiguration.defaultSimpleWorkflow()).thenReturn(graph);

    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setServiceInstances(Lists.newArrayList(inst1, inst2));
    executionArgs.setExecutionStrategy(ExecutionStrategy.PARALLEL);
    executionArgs.setCommandName("STOP");
    executionArgs.setWorkflowType(WorkflowType.SIMPLE);
    executionArgs.setServiceId(service.getUuid());

    WorkflowServiceImpl impl = (WorkflowServiceImpl) workflowService;
    impl.setStaticConfiguration(staticConfiguration);

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecutionServiceImpl workflowExecutionServiceImpl = (WorkflowExecutionServiceImpl) workflowExecutionService;
    WorkflowExecution workflowExecution =
        workflowExecutionServiceImpl.triggerEnvExecution(app.getUuid(), env.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(workflowExecution).isNotNull();
    assertThat(workflowExecution.getUuid()).isNotNull();

    WorkflowExecution workflowExecution2 =
        workflowExecutionService.getExecutionDetails(app.getUuid(), workflowExecution.getUuid());
    assertThat(workflowExecution2)
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.FAILED);

    assertThat(workflowExecution2.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByInstances")
        .hasFieldOrPropertyWithValue("type", "REPEAT")
        .hasFieldOrPropertyWithValue("status", "FAILED");
    assertThat(workflowExecution2.getExecutionNode().getGroup()).isNotNull();
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("host1:TEMPLATE_NAME", "host2:TEMPLATE_NAME");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("type")
        .contains("ELEMENT", "ELEMENT");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("stop", "stop");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("type")
        .contains("COMMAND", "COMMAND");
    assertThat(workflowExecution2.getExecutionNode().getGroup().getElements())
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("FAILED", "FAILED");

    PageRequest<WorkflowExecution> pageRequest = PageRequest.Builder.aPageRequest()
                                                     .addFilter("appId", Operator.EQ, app.getUuid())
                                                     .addFilter("uuid", Operator.EQ, workflowExecution.getUuid())
                                                     .build();
    PageResponse<WorkflowExecution> res = workflowExecutionService.listExecutions(pageRequest, true);
    assertThat(res).isNotNull().hasSize(1).doesNotContainNull();

    workflowExecution2 = res.get(0);
    assertThat(workflowExecution2)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getAppId, WorkflowExecution::getStateMachineId,
            WorkflowExecution::getWorkflowId)
        .containsExactly(workflowExecution.getUuid(), app.getUuid(), workflowExecution.getStateMachineId(),
            workflowExecution.getWorkflowId());
    assertThat(workflowExecution2.getStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  /**
   * Should trigger complex workflow.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerComplexWorkflow() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host host2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc2").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate1 = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service1.getUuid())
            .withName(service1.getName())
            .withAppId(app.getUuid())
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate1.setService(service1);
    ServiceTemplate serviceTemplate2 = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service2.getUuid())
            .withName(service2.getName())
            .withAppId(app.getUuid())
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate1.setService(service2);

    ServiceInstance.Builder builder1 =
        aServiceInstance().withServiceTemplate(serviceTemplate1).withAppId(app.getUuid()).withEnvId(env.getUuid());
    ServiceInstance.Builder builder2 =
        aServiceInstance().withServiceTemplate(serviceTemplate2).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst11 = serviceInstanceService.save(builder1.withHost(host1).build());
    ServiceInstance inst12 = serviceInstanceService.save(builder1.withHost(host2).build());
    ServiceInstance inst21 = serviceInstanceService.save(builder2.withHost(host1).build());
    ServiceInstance inst22 = serviceInstanceService.save(builder2.withHost(host2).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("svcRepeatWait")
                          .withName("svcRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instRepeatWait")
                          .withName("instRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instSuccessWait")
                          .withName("instSuccessWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(
                aLink().withId("l1").withFrom("Repeat By Services").withTo("svcRepeatWait").withType("repeat").build())
            .addLinks(
                aLink().withId("l2").withFrom("svcRepeatWait").withTo("RepeatByInstances").withType("success").build())
            .addLinks(
                aLink().withId("l3").withFrom("RepeatByInstances").withTo("instRepeatWait").withType("repeat").build())
            .addLinks(aLink()
                          .withId("l4")
                          .withFrom("RepeatByInstances")
                          .withTo("instSuccessWait")
                          .withType("success")
                          .build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getExecutionNode())
        .isNotNull()
        .extracting("name", "type", "status")
        .containsExactly("Repeat By Services", "REPEAT", "SUCCESS");
    assertThat(execution.getExecutionNode().getGroup()).isNotNull();
    assertThat(execution.getExecutionNode().getGroup().getElements()).isNotNull().doesNotContainNull().hasSize(2);

    List<Node> svcElements = execution.getExecutionNode().getGroup().getElements();
    assertThat(svcElements).isNotNull().hasSize(2).extracting("name").contains(service1.getName(), service2.getName());
    assertThat(svcElements).extracting("type").contains("ELEMENT", "ELEMENT");

    List<Node> svcRepeatWaits = svcElements.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(svcRepeatWaits).isNotNull().hasSize(2).extracting("name").contains("svcRepeatWait", "svcRepeatWait");
    assertThat(svcRepeatWaits).extracting("type").contains("WAIT", "WAIT");

    List<Node> repeatInstance = svcRepeatWaits.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(repeatInstance)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("RepeatByInstances", "RepeatByInstances");
    assertThat(repeatInstance).extracting("type").contains("REPEAT", "REPEAT");

    List<Node> instSuccessWait = repeatInstance.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(instSuccessWait)
        .isNotNull()
        .hasSize(2)
        .extracting("name")
        .contains("instSuccessWait", "instSuccessWait");
    assertThat(instSuccessWait).extracting("type").contains("WAIT", "WAIT");

    List<Node> instRepeatElements =
        repeatInstance.stream().map(Node::getGroup).flatMap(g -> g.getElements().stream()).collect(Collectors.toList());
    assertThat(instRepeatElements).extracting("type").contains("ELEMENT", "ELEMENT", "ELEMENT", "ELEMENT");

    List<Node> instRepeatWait = instRepeatElements.stream().map(Node::getNext).collect(Collectors.toList());
    assertThat(instRepeatWait)
        .isNotNull()
        .hasSize(4)
        .extracting("name")
        .contains("instRepeatWait", "instRepeatWait", "instRepeatWait", "instRepeatWait");
    assertThat(instRepeatWait).extracting("type").contains("WAIT", "WAIT", "WAIT", "WAIT");
  }

  /**
   * Trigger pipeline.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void triggerPipeline() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Host host = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst = serviceInstanceService.save(builder.withHost(host).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("Repeat By Services")
                          .withOrigin(true)
                          .withName("Repeat By Services")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("svcRepeatWait")
                          .withName("svcRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instRepeatWait")
                          .withName("instRepeatWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("instSuccessWait")
                          .withName("instSuccessWait")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(
                aLink().withId("l1").withFrom("Repeat By Services").withTo("svcRepeatWait").withType("repeat").build())
            .addLinks(
                aLink().withId("l2").withFrom("svcRepeatWait").withTo("RepeatByInstances").withType("success").build())
            .addLinks(
                aLink().withId("l3").withFrom("RepeatByInstances").withTo("instRepeatWait").withType("repeat").build())
            .addLinks(aLink()
                          .withId("l4")
                          .withFrom("RepeatByInstances")
                          .withTo("instSuccessWait")
                          .withType("success")
                          .build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    PipelineStage stag1 = new PipelineStage(asList(new PipelineStageElement("DEV", StateType.ENV_STATE.name(),
        ImmutableMap.of("envId", env.getUuid(), "workflowId", orchestration.getUuid()))));
    PipelineStage stag2 = new PipelineStage(asList(
        new PipelineStageElement("APPROVAL", StateType.APPROVAL.name(), ImmutableMap.of("envId", env.getUuid()))));
    List<PipelineStage> pipelineStages = asList(stag1, stag2);

    Pipeline pipeline = aPipeline()
                            .withAppId(app.getUuid())
                            .withName("pipeline1")
                            .withDescription("Sample Pipeline")
                            .withPipelineStages(pipelineStages)
                            .build();

    pipeline = pipelineService.createPipeline(pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull().hasSize(1).doesNotContainNull();
    assertThat(res.get(0).getTransitions()).hasSize(1);

    Artifact artifact = wingsPersistence.saveAndGet(Artifact.class,
        anArtifact()
            .withAppId(app.getUuid())
            .withDisplayName(ARTIFACT_NAME)
            .withServiceIds(asList(service.getUuid()))
            .build());
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(asList(artifact));

    triggerPipeline(app.getUuid(), pipeline, executionArgs);
  }

  private WorkflowExecution triggerPipeline(String appId, Pipeline pipeline, ExecutionArgs executionArgs)
      throws InterruptedException {
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerPipelineExecution(appId, pipeline.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    return execution;
  }

  /**
   * Should trigger orchestration.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerOrchestration() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("abc").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());
    triggerOrchestration(appId, env);
  }

  /**
   * Should get node details.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldGetNodeDetails() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("abc").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());

    WorkflowExecution execution = workflowExecutionService.getExecutionDetails(appId, triggerOrchestration(appId, env));
    Node node0 = execution.getExecutionNode();
    assertThat(workflowExecutionService.getExecutionDetailsForNode(appId, execution.getUuid(), node0.getId()))
        .isEqualToIgnoringGivenFields(node0, "x", "y", "width", "height", "next", "expanded");
  }

  /**
   * Should update in progress count.
   *
   * @throws InterruptedException the interrupted exception
   */
  //  @Test
  //  public void shouldUpdateInProgressCount() throws InterruptedException {
  //    Environment env = wingsPersistence.saveAndGet(Environment.class,
  //    Builder.anEnvironment().withAppId(appId).build()); triggerOrchestration(env); WorkflowExecution
  //    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    workflowExecutionService.incrementInProgressCount(workflowExecution.getAccountId(), workflowExecution.getUuid(),
  //    1); workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    assertThat(workflowExecution.getBreakdown().getInprogress()).isEqualTo(1);
  //  }

  /**
   * Should update success count.
   *
   * @throws InterruptedException the interrupted exception
   */
  //  @Test
  //  public void shouldUpdateSuccessCount() throws InterruptedException {
  //    Environment env = wingsPersistence.saveAndGet(Environment.class,
  //    Builder.anEnvironment().withAppId(appId).build()); triggerOrchestration(env); WorkflowExecution
  //    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    workflowExecutionService.incrementSuccess(workflowExecution.getAccountId(), workflowExecution.getUuid(), 1);
  //    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
  //    assertThat(workflowExecution.getBreakdown().getSuccess()).isEqualTo(2);
  //  }

  /**
   * Should update failed count.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldUpdateFailedCount() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("abc").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());
    triggerOrchestration(appId, env);
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
    workflowExecutionService.incrementFailed(workflowExecution.getAppId(), workflowExecution.getUuid(), 1);
    workflowExecution = wingsPersistence.get(WorkflowExecution.class, new PageRequest<>());
    assertThat(workflowExecution.getBreakdown().getFailed()).isEqualTo(1);
  }

  /**
   * Trigger orchestration.
   *
   * @param appId the app id
   * @param env   the env
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public String triggerOrchestration(String appId, Environment env) throws InterruptedException {
    Orchestration orchestration = createExecutableOrchestration(appId, env);
    ExecutionArgs executionArgs = new ExecutionArgs();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution =
        ((WorkflowExecutionServiceImpl) workflowExecutionService)
            .triggerOrchestrationExecution(appId, env.getUuid(), orchestration.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
  }

  private Orchestration createExecutableOrchestration(String appId, Environment env) {
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("n1")
                                    .withName("wait")
                                    .withX(200)
                                    .withY(50)
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1l)
                                    .withOrigin(true)
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
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(appId)
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
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
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("abc").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());

    triggerOrchestration(appId, env);

    // 2nd orchestration
    Orchestration orchestration = createExecutableOrchestration(appId, env);
    PageRequest<Orchestration> pageRequest = new PageRequest<>();
    PageResponse<Orchestration> res = workflowService.listOrchestration(pageRequest, null);

    assertThat(res).isNotNull().hasSize(2);
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldPauseAndResumeState() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("wait1")
                                    .withOrigin(true)
                                    .withName("wait1")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addNodes(aNode()
                                    .withId("pause1")
                                    .withName("pause1")
                                    .withType(StateType.PAUSE.name())
                                    .addProperty("toAddress", "to1")
                                    .build())
                      .addNodes(aNode()
                                    .withId("wait2")
                                    .withName("wait2")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("wait1").withTo("pause1").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("pause1").withTo("wait2").withType("success").build())
                      .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.PAUSED && i < 5);
    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "PAUSED");

    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withEnvId(env.getUuid())
                                        .withExecutionUuid(executionId)
                                        .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
                                        .withExecutionEventType(ExecutionEventType.RESUME)
                                        .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);
    workflowExecutionSignals.get(signalId).await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait2")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
  }

  /**
   * Should pause and resume
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldPauseAllAndResumeAllState() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc2").withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("wait1")
                          .withName("wait1")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("wait2")
                          .withName("wait2")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
            .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    Thread.sleep(1000);

    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withExecutionEventType(ExecutionEventType.PAUSE_ALL)
                                        .withExecutionUuid(executionId)
                                        .withEnvId(env.getUuid())
                                        .build();
    executionEvent = workflowExecutionService.triggerExecutionEvent(executionEvent);
    assertThat(executionEvent).isNotNull().hasFieldOrProperty("uuid");

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.PAUSED && i < 5);

    List<Node> wait1List = execution.getExecutionNode()
                               .getGroup()
                               .getElements()
                               .stream()
                               .filter(n -> n.getNext() != null)
                               .map(Node::getNext)
                               .collect(Collectors.toList());
    List<Node> wait2List =
        wait1List.stream().filter(n -> n.getNext() != null).map(Node::getNext).collect(Collectors.toList());

    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);
    assertThat(execution.getExecutionNode())
        .extracting("name", "type", "status")
        .containsExactly("RepeatByServices", "REPEAT", "RUNNING");
    assertThat(wait1List)
        .filteredOn("name", "wait1")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
    assertThat(wait2List)
        .filteredOn("name", "wait2")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "PAUSED".equals(n.getStatus()));

    executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                         .withAppId(app.getUuid())
                         .withExecutionEventType(ExecutionEventType.RESUME_ALL)
                         .withExecutionUuid(executionId)
                         .withEnvId(env.getUuid())
                         .build();
    executionEvent = workflowExecutionService.triggerExecutionEvent(executionEvent);
    assertThat(executionEvent).isNotNull().hasFieldOrProperty("uuid");

    workflowExecutionSignals.get(signalId).await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.SUCCESS);

    wait1List = execution.getExecutionNode()
                    .getGroup()
                    .getElements()
                    .stream()
                    .filter(n -> n.getNext() != null)
                    .map(Node::getNext)
                    .collect(Collectors.toList());
    wait2List = wait1List.stream().filter(n -> n.getNext() != null).map(Node::getNext).collect(Collectors.toList());

    assertThat(execution.getExecutionNode())
        .extracting("name", "type", "status")
        .containsExactly("RepeatByServices", "REPEAT", "SUCCESS");
    assertThat(wait1List)
        .filteredOn("name", "wait1")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
    assertThat(wait2List)
        .filteredOn("name", "wait2")
        .hasSize(2)
        .allMatch(n -> "WAIT".equals(n.getType()) && "SUCCESS".equals(n.getStatus()));
  }

  /**
   * Should throw invalid argument for invalid orchestration id.
   */
  @Test
  public void shouldThrowInvalidArgumentForInvalidOrchestrationId() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withExecutionEventType(ExecutionEventType.PAUSE)
                                        .withEnvId(env.getUuid())
                                        .withExecutionUuid(UUIDGenerator.getUuid())
                                        .build();
    try {
      executionEvent = workflowExecutionService.triggerExecutionEvent(executionEvent);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception.getParams().values()).doesNotContainNull();
      assertThat(exception.getParams().values().iterator().next())
          .isInstanceOf(String.class)
          .asString()
          .startsWith("no workflowExecution for executionUuid");
      assertThat(exception).hasMessage(ErrorCodes.INVALID_ARGUMENT.getCode());
    }
  }

  /**
   * Should abort
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldAbortState() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("wait1")
                                    .withOrigin(true)
                                    .withName("wait1")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addNodes(aNode()
                                    .withId("pause1")
                                    .withName("pause1")
                                    .withType(StateType.PAUSE.name())
                                    .addProperty("toAddress", "to1")
                                    .build())
                      .addNodes(aNode()
                                    .withId("wait2")
                                    .withName("wait2")
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1)
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("wait1").withTo("pause1").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("pause1").withTo("wait2").withType("success").build())
                      .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.PAUSED && i < 5);
    assertThat(execution).isNotNull().extracting("uuid", "status").containsExactly(executionId, ExecutionStatus.PAUSED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "PAUSED");

    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withEnvId(env.getUuid())
                                        .withExecutionUuid(executionId)
                                        .withStateExecutionInstanceId(execution.getExecutionNode().getNext().getId())
                                        .withExecutionEventType(ExecutionEventType.ABORT)
                                        .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);
    workflowExecutionSignals.get(signalId).await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "wait1")
        .hasFieldOrPropertyWithValue("status", "SUCCESS");
    assertThat(execution.getExecutionNode().getNext())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "pause1")
        .hasFieldOrPropertyWithValue("status", "ABORTED");
  }

  /**
   * Should abort all
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldAbortAllStates() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Service service1 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    Service service2 = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc2").withAppId(app.getUuid()).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("wait1")
                          .withName("wait1")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addNodes(aNode()
                          .withId("wait2")
                          .withName("wait2")
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 1)
                          .build())
            .addLinks(aLink().withId("l1").withFrom("RepeatByServices").withTo("wait1").withType("repeat").build())
            .addLinks(aLink().withId("l2").withFrom("wait1").withTo("wait2").withType("success").build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    Thread.sleep(1000);

    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withExecutionEventType(ExecutionEventType.ABORT_ALL)
                                        .withExecutionUuid(executionId)
                                        .withEnvId(env.getUuid())
                                        .build();
    executionEvent = workflowExecutionService.triggerExecutionEvent(executionEvent);
    assertThat(executionEvent).isNotNull().hasFieldOrProperty("uuid");

    int i = 0;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    } while (execution.getStatus() != ExecutionStatus.ABORTED && i < 5);

    assertThat(execution)
        .isNotNull()
        .extracting("uuid", "status")
        .containsExactly(executionId, ExecutionStatus.ABORTED);

    assertThat(execution.getExecutionNode())
        .isNotNull()
        .hasFieldOrPropertyWithValue("name", "RepeatByServices")
        .hasFieldOrPropertyWithValue("status", "ABORTED");
    assertThat(execution.getExecutionNode().getGroup()).isNotNull();
    assertThat(execution.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("next")
        .doesNotContainNull()
        .extracting("name")
        .contains("wait1", "wait1");
    assertThat(execution.getExecutionNode().getGroup().getElements())
        .isNotNull()
        .hasSize(2)
        .extracting("next")
        .doesNotContainNull()
        .extracting("status")
        .contains("ABORTED", "ABORTED");
  }

  /**
   * Should pause on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldPauseOnError() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Host applicationHost1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());
    Host applicationHost2 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host2").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(applicationHost1).build());
    ServiceInstance inst2 = serviceInstanceService.save(builder.withHost(applicationHost2).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("install")
                          .withName("install")
                          .withType(StateType.COMMAND.name())
                          .addProperty("command", "install")
                          .build())
            .addLinks(aLink()
                          .withId("l1")
                          .withFrom("RepeatByServices")
                          .withTo("RepeatByInstances")
                          .withType("repeat")
                          .build())
            .addLinks(aLink().withId("l2").withFrom("RepeatByInstances").withTo("install").withType("repeat").build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    List<Node> installNodes = null;
    boolean paused = false;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

      installNodes = execution.getExecutionNode()
                         .getGroup()
                         .getElements()
                         .stream()
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .filter(n -> n.getGroup() != null)
                         .map(Node::getGroup)
                         .filter(g -> g.getElements() != null)
                         .flatMap(g -> g.getElements().stream())
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .collect(Collectors.toList());
      paused = !installNodes.stream()
                    .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.PAUSED_ON_ERROR.name()))
                    .collect(Collectors.toList())
                    .isEmpty();
    } while (!paused && i < 5);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.PAUSED);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.PAUSED_ON_ERROR.name());

    Node installNode = installNodes.get(0);
    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withEnvId(env.getUuid())
                                        .withExecutionUuid(executionId)
                                        .withStateExecutionInstanceId(installNode.getId())
                                        .withExecutionEventType(ExecutionEventType.RESUME)
                                        .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);

    i = 0;
    installNodes = null;
    paused = false;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

      installNodes = execution.getExecutionNode()
                         .getGroup()
                         .getElements()
                         .stream()
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .filter(n -> n.getGroup() != null)
                         .map(Node::getGroup)
                         .filter(g -> g.getElements() != null)
                         .flatMap(g -> g.getElements().stream())
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .collect(Collectors.toList());
      paused = !installNodes.stream()
                    .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.PAUSED_ON_ERROR.name()))
                    .collect(Collectors.toList())
                    .isEmpty();
    } while (!paused && i < 5);

    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.PAUSED);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .contains(ExecutionStatus.SUCCESS.name(), ExecutionStatus.PAUSED_ON_ERROR.name());

    installNode =
        installNodes.stream()
            .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.PAUSED_ON_ERROR.name()))
            .collect(Collectors.toList())
            .get(0);
    executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                         .withAppId(app.getUuid())
                         .withEnvId(env.getUuid())
                         .withExecutionUuid(executionId)
                         .withStateExecutionInstanceId(installNode.getId())
                         .withExecutionEventType(ExecutionEventType.RESUME)
                         .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);
    workflowExecutionSignals.get(signalId).await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);
    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(n -> n.getNext() != null)
                       .map(Node::getNext)
                       .filter(n -> n.getGroup() != null)
                       .map(Node::getGroup)
                       .filter(g -> g.getElements() != null)
                       .flatMap(g -> g.getElements().stream())
                       .filter(n -> n.getNext() != null)
                       .map(Node::getNext)
                       .collect(Collectors.toList());
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(2)
        .extracting("status")
        .containsExactly(ExecutionStatus.SUCCESS.name(), ExecutionStatus.SUCCESS.name());
  }

  /**
   * Should retry on error
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldRetryOnError() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Host host1 = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getUuid()).withEnvId(env.getUuid()).withHostName("host1").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());
    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    software.wings.beans.ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance inst1 = serviceInstanceService.save(builder.withHost(host1).build());

    Graph graph =
        aGraph()
            .addNodes(aNode()
                          .withId("RepeatByServices")
                          .withOrigin(true)
                          .withName("RepeatByServices")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${services()}")
                          .addProperty("executionStrategy", ExecutionStrategy.PARALLEL)
                          .build())
            .addNodes(aNode()
                          .withId("RepeatByInstances")
                          .withName("RepeatByInstances")
                          .withType(StateType.REPEAT.name())
                          .addProperty("repeatElementExpression", "${instances()}")
                          .addProperty("executionStrategy", ExecutionStrategy.SERIAL)
                          .build())
            .addNodes(aNode()
                          .withId("install")
                          .withName("install")
                          .withType(StateType.COMMAND.name())
                          .addProperty("command", "install")
                          .build())
            .addLinks(aLink()
                          .withId("l1")
                          .withFrom("RepeatByServices")
                          .withTo("RepeatByInstances")
                          .withType("repeat")
                          .build())
            .addLinks(aLink().withId("l2").withFrom("RepeatByInstances").withTo("install").withType("repeat").build())
            .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(app.getUuid())
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withGraph(graph)
                                      .withWorkflowType(WorkflowType.ORCHESTRATION)
                                      .withTargetToAllEnv(true)
                                      .build();
    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setErrorStrategy(ErrorStrategy.PAUSE);
    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationExecution(app.getUuid(), env.getUuid(),
                                          orchestration.getUuid(), executionArgs, callback);

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();

    int i = 0;
    List<Node> installNodes = null;
    boolean paused = false;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

      installNodes = execution.getExecutionNode()
                         .getGroup()
                         .getElements()
                         .stream()
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .filter(n -> n.getGroup() != null)
                         .map(Node::getGroup)
                         .filter(g -> g.getElements() != null)
                         .flatMap(g -> g.getElements().stream())
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .collect(Collectors.toList());
      paused = !installNodes.stream()
                    .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.PAUSED_ON_ERROR.name()))
                    .collect(Collectors.toList())
                    .isEmpty();
    } while (!paused && i < 5);

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.PAUSED);

    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.PAUSED_ON_ERROR.name());

    Node installNode = installNodes.get(0);
    ExecutionEvent executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                                        .withAppId(app.getUuid())
                                        .withEnvId(env.getUuid())
                                        .withExecutionUuid(executionId)
                                        .withStateExecutionInstanceId(installNode.getId())
                                        .withExecutionEventType(ExecutionEventType.RETRY)
                                        .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);

    i = 0;
    installNodes = null;
    paused = false;
    do {
      i++;
      Thread.sleep(1000);
      execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);

      installNodes = execution.getExecutionNode()
                         .getGroup()
                         .getElements()
                         .stream()
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .filter(n -> n.getGroup() != null)
                         .map(Node::getGroup)
                         .filter(g -> g.getElements() != null)
                         .flatMap(g -> g.getElements().stream())
                         .filter(n -> n.getNext() != null)
                         .map(Node::getNext)
                         .collect(Collectors.toList());
      paused = !installNodes.stream()
                    .filter(n -> n.getStatus() != null && n.getStatus().equals(ExecutionStatus.PAUSED_ON_ERROR.name()))
                    .collect(Collectors.toList())
                    .isEmpty();
    } while (!paused && i < 5);

    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.PAUSED);
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.PAUSED_ON_ERROR.name());

    installNode = installNodes.get(0);
    executionEvent = ExecutionEvent.Builder.aWorkflowExecutionEvent()
                         .withAppId(app.getUuid())
                         .withEnvId(env.getUuid())
                         .withExecutionUuid(executionId)
                         .withStateExecutionInstanceId(installNode.getId())
                         .withExecutionEventType(ExecutionEventType.RESUME)
                         .build();
    workflowExecutionService.triggerExecutionEvent(executionEvent);
    workflowExecutionSignals.get(signalId).await();

    execution = workflowExecutionService.getExecutionDetails(app.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", executionId)
        .hasFieldOrPropertyWithValue("status", ExecutionStatus.SUCCESS);

    installNodes = execution.getExecutionNode()
                       .getGroup()
                       .getElements()
                       .stream()
                       .filter(n -> n.getNext() != null)
                       .map(Node::getNext)
                       .filter(n -> n.getGroup() != null)
                       .map(Node::getGroup)
                       .filter(g -> g.getElements() != null)
                       .flatMap(g -> g.getElements().stream())
                       .filter(n -> n.getNext() != null)
                       .map(Node::getNext)
                       .collect(Collectors.toList());
    assertThat(installNodes)
        .isNotNull()
        .doesNotContainNull()
        .filteredOn("name", "install")
        .hasSize(1)
        .extracting("status")
        .containsExactly(ExecutionStatus.SUCCESS.name());
  }

  @Test
  public void shouldTriggerOrchestrationWorkflow() throws InterruptedException {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, aService().withUuid(UUIDGenerator.getUuid()).withName("svc1").withAppId(app.getUuid()).build());

    ServiceTemplate serviceTemplate = wingsPersistence.saveAndGet(ServiceTemplate.class,
        aServiceTemplate()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceId(service.getUuid())
            .withName("TEMPLATE_NAME")
            .withDescription("TEMPLATE_DESCRIPTION")
            .build());
    serviceTemplate.setService(service);

    wingsPersistence.saveAndGet(InfrastructureMapping.class,
        PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
            .withAppId(app.getUuid())
            .withEnvId(env.getUuid())
            .withServiceTemplateId(serviceTemplate.getUuid())
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .build());

    triggerOrchestrationWorkflow(app.getAppId(), env, service);
  }

  /**
   * Trigger orchestration.
   *
   * @param appId the app id
   * @param env   the env
   * @param service
   * @return the string
   * @throws InterruptedException the interrupted exception
   */
  public String triggerOrchestrationWorkflow(String appId, Environment env, Service service)
      throws InterruptedException {
    OrchestrationWorkflow orchestration = createOrchestrationWorkflow(appId, env, service);
    ExecutionArgs executionArgs = new ExecutionArgs();

    String signalId = UUIDGenerator.getUuid();
    WorkflowExecutionUpdateMock callback = new WorkflowExecutionUpdateMock(signalId);
    workflowExecutionSignals.put(signalId, new CountDownLatch(1));
    WorkflowExecution execution = ((WorkflowExecutionServiceImpl) workflowExecutionService)
                                      .triggerOrchestrationWorkflowExecution(
                                          appId, env.getUuid(), orchestration.getUuid(), executionArgs, callback);
    workflowExecutionSignals.get(signalId).await();

    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    execution = workflowExecutionService.getExecutionDetails(appId, executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
    return executionId;
  }

  private OrchestrationWorkflow createOrchestrationWorkflow(String appId, Environment env, Service service) {
    OrchestrationWorkflow orchestrationWorkflow =
        anOrchestrationWorkflow()
            .withAppId(appId)
            .withEnvironmentId(env.getUuid())
            .withWorkflowOrchestrationType(WorkflowOrchestrationType.CANARY)
            .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
            .addWorkflowPhases(aWorkflowPhase()
                                   .withName("Phase1")
                                   .withComputeProviderId(COMPUTE_PROVIDER_ID)
                                   .withServiceId(service.getUuid())
                                   .withDeploymentType(SSH)
                                   .build())
            .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
            .build();

    OrchestrationWorkflow orchestrationWorkflow2 = workflowService.createOrchestrationWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");

    OrchestrationWorkflow orchestrationWorkflow3 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = orchestrationWorkflow3.getWorkflowPhases().get(0);
    PhaseStep deployPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE)
                                    .collect(Collectors.toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(aNode().withType("HTTP").addProperty("url", "www.google.com").build());

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid(), workflowPhase);

    OrchestrationWorkflow orchestrationWorkflow4 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());

    logger.info("Graph Json : \n {}", JsonUtils.asJson(orchestrationWorkflow4.getGraph()));

    return orchestrationWorkflow4;
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
