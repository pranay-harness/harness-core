package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineTest;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;

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
  public void testSave() {
    StateMachine sm = createSynchSM(workflowService);
    System.out.println("All Done!");
  }

  /**
   * @param svc
   * @return
   */
  private StateMachine createSynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateA();
    sm.addState(stateA);
    StateMachineTest.StateB stateB = new StateMachineTest.StateB();
    sm.addState(stateB);
    StateMachineTest.StateC stateC = new StateMachineTest.StateC();
    sm.addState(stateC);
    sm.setInitialStateName(StateMachineTest.StateA.class.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  @Test
  public void testRead() throws InterruptedException {
    StateMachine sm = createSynchSM(workflowService);
    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    System.out.println("All Done!");
  }

  @Test
  public void testTrigger() throws InterruptedException {
    StateMachine sm = createSynchSM(workflowService);
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(5000);
  }

  @Test
  public void testTriggerAsynch() throws InterruptedException {
    StateMachine sm = createAsynchSM(workflowService);
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(30000);
  }

  /**
   * @param svc
   * @return
   */
  private StateMachine createAsynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    State stateA = new StateMachineTest.StateA();
    sm.addState(stateA);
    StateMachineTest.StateB stateB = new StateMachineTest.StateB();
    sm.addState(stateB);
    StateMachineTest.StateC stateC = new StateMachineTest.StateC();
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB", 10000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC", 5000);
    sm.addState(stateBC);

    sm.setInitialStateName(StateMachineTest.StateA.class.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.addTransition(new Transition(stateAB, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateBC));
    sm.addTransition(new Transition(stateBC, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }
}
