package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.WingsBootstrap;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateA;
import software.wings.sm.StateAsynch;
import software.wings.sm.StateB;
import software.wings.sm.StateC;
import software.wings.sm.StateMachine;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;

import javax.inject.Inject;

/**
 *
 */

/**
 * @author Rishi
 *
 */
public class TestWorkflowServiceImpl extends WingsBaseTest {
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
    State stateA = new StateA();
    sm.addState(stateA);
    StateB stateB = new StateB();
    sm.addState(stateB);
    StateC stateC = new StateC();
    sm.addState(stateC);
    sm.setInitialStateName(StateA.class.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  /**
   * @param svc
   * @return
   */
  private StateMachine createAsynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    State stateA = new StateA();
    sm.addState(stateA);
    StateB stateB = new StateB();
    sm.addState(stateB);
    StateC stateC = new StateC();
    sm.addState(stateC);

    State stateAB = new StateAsynch("StateAB", 10000);
    sm.addState(stateAB);
    State stateBC = new StateAsynch("StateBC", 5000);
    sm.addState(stateBC);

    sm.setInitialStateName(StateA.class.getName());

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
}
