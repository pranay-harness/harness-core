package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ForkState;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineTest;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

/**
 *
 */

/**
 * @author Rishi
 */
public class WorkflowServiceTest extends WingsBaseTest {
  @Inject private WorkflowService workflowService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void testRead() throws InterruptedException {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    System.out.println("All Done!");
  }

  @Test
  public void testTrigger() throws InterruptedException {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);
    Thread.sleep(5000);

    assertNotNull(StaticMap.getValue(stateA.getName()));
    assertNotNull(StaticMap.getValue(stateB.getName()));
    assertNotNull(StaticMap.getValue(stateC.getName()));

    assertThat(true)
        .as("StateA executed before StateB")
        .isEqualTo((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()));
    assertThat(true)
        .as("StateB executed before StateC")
        .isEqualTo((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateC.getName()));
  }

  @Test
  public void testTriggerAsynch() throws InterruptedException {
    StateMachine sm = createAsynchSM(workflowService);
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(30000);
  }

  private StateMachine createAsynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB", 10000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC", 5000);
    sm.addState(stateBC);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.addTransition(new Transition(stateAB, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateBC));
    sm.addTransition(new Transition(stateBC, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  @Test
  public void testTriggerSimpleFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
    forkStates.add(stateB.getName());
    forkStates.add(stateC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, fork1));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(5000);
  }

  @Test
  public void testTriggerMixedFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB", 5000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC", 2000);
    sm.addState(stateBC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
    forkStates.add(stateB.getName());
    forkStates.add(stateBC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.addTransition(new Transition(stateAB, TransitionType.SUCCESS, fork1));
    sm.addTransition(new Transition(fork1, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(10000);
  }
}
