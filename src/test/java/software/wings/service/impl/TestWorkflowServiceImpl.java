package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import software.wings.dl.MongoConnectionFactory;
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

/**
 *
 */

/**
 * @author Rishi
 *
 */
public class TestWorkflowServiceImpl {
  private static Injector injector;
  private static WingsPersistence wingsPersistence;

  @BeforeClass
  public static void setup() {
    MongoConnectionFactory factory = new MongoConnectionFactory();
    factory.setDb("test");
    factory.setHost("localhost");
    factory.setPort(27017);

    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MongoConnectionFactory.class).toInstance(factory);
        bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
        bind(WorkflowService.class).to(WorkflowServiceImpl.class);
      }
    });
    wingsPersistence = injector.getInstance(WingsPersistence.class);
  }

  @Test
  public void testSave() {
    WorkflowService svc = injector.getInstance(WorkflowService.class);

    StateMachine sm = createSynchSM(svc);
    System.out.println("All Done!");
  }

  /**
   * @param svc
   * @return
   */
  private StateMachine createSynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    State stateA = new StateA();
    sm.getStates().add(stateA);
    StateB stateB = new StateB();
    sm.getStates().add(stateB);
    StateC stateC = new StateC();
    sm.getStates().add(stateC);
    sm.setInitialStateName(StateA.class.getName());

    sm.getTransitions().add(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.getTransitions().add(new Transition(stateB, TransitionType.SUCCESS, stateC));

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
    sm.getStates().add(stateA);
    StateB stateB = new StateB();
    sm.getStates().add(stateB);
    StateC stateC = new StateC();
    sm.getStates().add(stateC);

    State stateAB = new StateAsynch("StateAB", 10000);
    sm.getStates().add(stateAB);
    State stateBC = new StateAsynch("StateBC", 5000);
    sm.getStates().add(stateBC);

    sm.setInitialStateName(StateA.class.getName());

    sm.getTransitions().add(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.getTransitions().add(new Transition(stateAB, TransitionType.SUCCESS, stateB));
    sm.getTransitions().add(new Transition(stateB, TransitionType.SUCCESS, stateBC));
    sm.getTransitions().add(new Transition(stateBC, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  @Test
  public void testRead() throws InterruptedException {
    WorkflowService svc = injector.getInstance(WorkflowService.class);
    StateMachine sm = createSynchSM(svc);
    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    System.out.println("All Done!");
  }

  @Test
  public void testTrigger() throws InterruptedException {
    WorkflowService svc = injector.getInstance(WorkflowService.class);
    StateMachine sm = createSynchSM(svc);
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    svc.trigger(smId);

    Thread.sleep(5000);
  }

  @Test
  public void testTriggerAsynch() throws InterruptedException {
    WorkflowService svc = injector.getInstance(WorkflowService.class);
    StateMachine sm = createAsynchSM(svc);
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    svc.trigger(smId);

    Thread.sleep(300000);
  }

  @AfterClass
  public static void close() {
    wingsPersistence.close();
  }
}
