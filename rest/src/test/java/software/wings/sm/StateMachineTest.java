package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static software.wings.sm.states.RepeatState.Builder.aRepeatState;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionStrategy;
import software.wings.common.InstanceExpressionProcessor;
import software.wings.common.thread.ThreadPool;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.RepeatState;
import software.wings.waitnotify.NotifyEventListener;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.StringNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Class StateMachineTest.
 */
@Listeners(NotifyEventListener.class)
public class StateMachineTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineTest.class);

  /**
   * Should validate.
   */
  @Test
  public void shouldValidate() {
    StateMachine sm = new StateMachine();
    State state = new StateSync("StateA");
    sm.addState(state);
    state = new StateSync("StateB");
    sm.addState(state);
    state = new StateSync("StateC");
    sm.addState(state);
    sm.setInitialStateName("StateA");
    assertThat(sm.validate()).as("Validate result").isTrue();
  }

  /**
   * Should throw dup error code.
   */
  @Test
  public void shouldThrowDupErrorCode() {
    try {
      StateMachine sm = new StateMachine();
      State state = new StateSync("StateA");
      sm.addState(state);
      state = new StateSync("StateB");
      sm.addState(state);
      state = new StateSync("StateC");
      sm.addState(state);
      sm.setInitialStateName("StateA");
      state = new StateSync("StateB");
      sm.addState(state);
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.DUPLICATE_STATE_NAMES.name());
    }
  }

  /**
   * Should throw null transition.
   */
  @Test
  public void shouldThrowNullTransition() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition().withToState(stateA).withFromState(stateB).build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.TRANSITION_TYPE_NULL.name());
    }
  }

  /**
   * Should throw transition not linked.
   */
  @Test
  public void shouldThrowTransitionNotLinked() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateA)
                           .withFromState(stateB)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateB)
                           .withFromState(null)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.TRANSITION_NOT_LINKED.name());
    }
  }

  /**
   * Should throw transition to incorrect state.
   */
  @Test
  public void shouldThrowTransitionToIncorrectState() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      sm.setInitialStateName("StateA");

      StateSync stateC = new StateSync("StateC");
      StateSync stateD = new StateSync("StateD");
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateA)
                           .withFromState(stateB)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateD)
                           .withFromState(stateC)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.TRANSITION_TO_INCORRECT_STATE.name());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("invalidStateNames");
      assertThat(exception.getParams().get("invalidStateNames")).asString().contains("StateC").contains("StateD");
    }
  }

  /**
   * Should throw states with Dup transitions.
   */
  @Test
  public void shouldThrowStatesWithDupTransitions() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      StateSync stateC = new StateSync("StateC");
      sm.addState(stateC);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateB)
                           .withFromState(stateA)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateC)
                           .withFromState(stateA)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateB)
                           .withFromState(stateC)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withToState(stateA)
                           .withFromState(stateC)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.STATES_WITH_DUP_TRANSITIONS.name());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("statesWithDupTransitions");
      assertThat(exception.getParams().get("statesWithDupTransitions"))
          .asString()
          .contains("StateA")
          .contains("StateC");
    }
  }

  /**
   * Should throw non-fork state transition.
   */
  @Test
  public void shouldThrowNonForkStateTransitions() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      StateSync stateC = new StateSync("StateC");
      sm.addState(stateC);
      StateSync stateD = new StateSync("StateD");
      sm.addState(stateD);
      ForkState fork1 = new ForkState("fork1");
      sm.addState(fork1);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateA)
                           .withToState(stateB)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateB)
                           .withToState(fork1)
                           .withTransitionType(TransitionType.FORK)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(fork1)
                           .withToState(stateC)
                           .withTransitionType(TransitionType.FORK)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateC)
                           .withToState(stateD)
                           .withTransitionType(TransitionType.FORK)
                           .build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.NON_FORK_STATES.name());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("nonForkStates");
      assertThat(exception.getParams().get("nonForkStates")).asString().contains("StateB").contains("StateC");
    }
  }

  /**
   * Should throw non-repeat state transition.
   */
  @Test
  public void shouldThrowNonRepeatStateTransitions() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSync("StateA");
      sm.addState(stateA);
      StateSync stateB = new StateSync("StateB");
      sm.addState(stateB);
      StateSync stateC = new StateSync("StateC");
      sm.addState(stateC);
      StateSync stateD = new StateSync("StateD");
      sm.addState(stateD);
      RepeatState repeat1 = new RepeatState("repeat1");
      sm.addState(repeat1);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateA)
                           .withToState(stateB)
                           .withTransitionType(TransitionType.SUCCESS)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateB)
                           .withToState(repeat1)
                           .withTransitionType(TransitionType.REPEAT)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(repeat1)
                           .withToState(stateC)
                           .withTransitionType(TransitionType.REPEAT)
                           .build());
      sm.addTransition(Transition.Builder.aTransition()
                           .withFromState(stateC)
                           .withToState(stateD)
                           .withTransitionType(TransitionType.REPEAT)
                           .build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.NON_REPEAT_STATES.name());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("nonRepeatStates");
      assertThat(exception.getParams().get("nonRepeatStates")).asString().contains("StateB").contains("StateC");
    }
  }

  /**
   * Should throw non-repeat state transition.
   */
  @Test
  public void shouldExpandRepeatState() {
    StateMachine sm = new StateMachine();
    State starting = new StateSync("starting");
    sm.addState(starting);
    RepeatState repeatByService1 = aRepeatState()
                                       .withRepeatElementExpression("services()")
                                       .withName("RepeatByServices")
                                       .withExecutionStrategy(ExecutionStrategy.SERIAL)
                                       .withRepeatElementType(ContextElementType.SERVICE)
                                       .build();
    sm.addState(repeatByService1);
    StateSync runCommand = new StateSync("command");
    runCommand.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand);
    StateSync finished = new StateSync("finished");
    sm.addState(finished);
    sm.setInitialStateName("starting");

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(starting)
                         .withToState(repeatByService1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByService1)
                         .withToState(runCommand)
                         .withTransitionType(TransitionType.REPEAT)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByService1)
                         .withToState(finished)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.validate();

    sm.addRepeatersForRequiredContextElement();

    sm.clearCache();

    sm.validate();

    RepeatState expectedNewState = aRepeatState()
                                       .withName("Repeat " + runCommand.getName())
                                       .withRepeatElementType(ContextElementType.INSTANCE)
                                       .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                                       .withRepeatElementExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION)
                                       .withRepeatTransitionStateName(runCommand.getName())
                                       .build();

    assertThat(sm.getStates()).hasSize(5).contains(expectedNewState);
    assertThat(sm.getNextStates(expectedNewState.getName(), TransitionType.REPEAT)).hasSize(1).containsOnly(runCommand);
    assertThat(sm.getNextStates("RepeatByServices", TransitionType.SUCCESS)).hasSize(1).containsOnly(finished);
  }

  /**
   * Should expand repeat state in a complex scenario.
   */
  @Test
  public void shouldExpandRepeatStateInMultiplePaths() {
    StateMachine sm = new StateMachine();
    State starting = new StateSync("starting");
    sm.addState(starting);
    RepeatState repeatByService1 = aRepeatState()
                                       .withRepeatElementExpression("services()")
                                       .withName("RepeatByServices")
                                       .withExecutionStrategy(ExecutionStrategy.SERIAL)
                                       .withRepeatElementType(ContextElementType.SERVICE)
                                       .build();
    sm.addState(repeatByService1);
    RepeatState repeatByHosts1 = aRepeatState()
                                     .withRepeatElementExpression("host()")
                                     .withName("RepeatByHosts")
                                     .withExecutionStrategy(ExecutionStrategy.SERIAL)
                                     .withRepeatElementType(ContextElementType.HOST)
                                     .build();
    sm.addState(repeatByHosts1);
    StateSync runCommand = new StateSync("command");
    runCommand.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand);
    StateSync finished = new StateSync("finished");
    sm.addState(finished);
    sm.setInitialStateName("starting");

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(starting)
                         .withToState(repeatByService1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(starting)
                         .withToState(repeatByHosts1)
                         .withTransitionType(TransitionType.FAILURE)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByService1)
                         .withToState(runCommand)
                         .withTransitionType(TransitionType.REPEAT)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByService1)
                         .withToState(finished)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByHosts1)
                         .withToState(runCommand)
                         .withTransitionType(TransitionType.REPEAT)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByHosts1)
                         .withToState(finished)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.validate();

    sm.addRepeatersForRequiredContextElement();

    sm.clearCache();

    sm.validate();

    RepeatState expectedNewState = aRepeatState()
                                       .withName("Repeat " + runCommand.getName())
                                       .withRepeatElementType(ContextElementType.INSTANCE)
                                       .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                                       .withRepeatElementExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION)
                                       .withRepeatTransitionStateName(runCommand.getName())
                                       .build();

    assertThat(sm.getStates()).hasSize(6).contains(expectedNewState);
    assertThat(sm.getNextStates("RepeatByServices", TransitionType.REPEAT)).hasSize(1).containsOnly(expectedNewState);
    assertThat(sm.getNextStates("RepeatByServices", TransitionType.SUCCESS)).hasSize(1).containsOnly(finished);
    assertThat(sm.getNextStates("RepeatByHosts", TransitionType.REPEAT)).hasSize(1).containsOnly(expectedNewState);
    assertThat(sm.getNextStates("RepeatByHosts", TransitionType.SUCCESS)).hasSize(1).containsOnly(finished);
    assertThat(sm.getNextStates("RepeatByServices", TransitionType.REPEAT).get(0))
        .isSameAs(sm.getNextStates("RepeatByHosts", TransitionType.REPEAT).get(0));
  }

  /**
   * Should expand repeat state for each node that needs it.
   */
  @Test
  public void shouldExpandRepeatStateForEachNodeThatNeedsIt() {
    StateMachine sm = new StateMachine();
    State starting = new StateSync("starting");
    sm.addState(starting);
    RepeatState repeatByService1 = aRepeatState()
                                       .withRepeatElementExpression("services()")
                                       .withName("RepeatByServices")
                                       .withExecutionStrategy(ExecutionStrategy.SERIAL)
                                       .withRepeatElementType(ContextElementType.SERVICE)
                                       .build();
    sm.addState(repeatByService1);
    StateSync runCommand1 = new StateSync("command1");
    runCommand1.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand1);
    StateSync runCommand2 = new StateSync("command2");
    runCommand2.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand2);
    StateSync runCommand3 = new StateSync("command3");
    runCommand3.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand3);
    StateSync finished = new StateSync("finished");
    finished.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(finished);
    sm.setInitialStateName("RepeatByServices");

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(repeatByService1)
                         .withToState(starting)
                         .withTransitionType(TransitionType.REPEAT)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(starting)
                         .withToState(runCommand1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand1)
                         .withToState(runCommand2)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand2)
                         .withToState(runCommand3)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand3)
                         .withToState(finished)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.validate();

    sm.addRepeatersForRequiredContextElement();

    sm.clearCache();

    sm.validate();

    RepeatState expectedNewState = aRepeatState()
                                       .withName("Repeat " + runCommand1.getName())
                                       .withRepeatElementType(ContextElementType.INSTANCE)
                                       .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                                       .withRepeatElementExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION)
                                       .withRepeatTransitionStateName(runCommand1.getName())
                                       .build();

    assertThat(sm.getStates()).hasSize(7);
    assertThat(sm.getNextStates("RepeatByServices", TransitionType.REPEAT)).hasSize(1).containsOnly(starting);
    assertThat(sm.getNextStates("starting", TransitionType.SUCCESS)).hasSize(1).containsOnly(expectedNewState);
    assertThat(sm.getNextStates(expectedNewState.getName(), TransitionType.REPEAT))
        .hasSize(1)
        .containsOnly(runCommand1);
    assertThat(sm.getNextStates("command1", TransitionType.SUCCESS)).hasSize(1).containsOnly(runCommand2);
    assertThat(sm.getNextStates("command2", TransitionType.SUCCESS)).hasSize(1).containsOnly(runCommand3);
    assertThat(sm.getNextStates("command3", TransitionType.SUCCESS)).hasSize(1).containsOnly(finished);
  }

  @Test
  public void shouldBreakRepeatTransitionsWhenNoNeeded() {
    StateMachine sm = new StateMachine();
    State starting = new StateSync("starting");
    sm.addState(starting);
    StateSync runCommand1 = new StateSync("command1");
    runCommand1.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand1);
    StateSync runCommand2 = new StateSync("command2");
    sm.addState(runCommand2);
    StateSync runCommand3 = new StateSync("command3");
    runCommand3.setRequiredContextElementType(ContextElementType.INSTANCE);
    sm.addState(runCommand3);
    StateSync finished = new StateSync("finished");
    sm.addState(finished);
    sm.setInitialStateName(starting.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(starting)
                         .withToState(runCommand1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand1)
                         .withToState(runCommand2)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand2)
                         .withToState(runCommand3)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(runCommand3)
                         .withToState(finished)
                         .withTransitionType(TransitionType.SUCCESS)
                         .build());
    sm.validate();

    sm.addRepeatersForRequiredContextElement();

    sm.clearCache();

    sm.validate();

    RepeatState expectedNewState1 = aRepeatState()
                                        .withName("Repeat " + runCommand1.getName())
                                        .withRepeatElementType(ContextElementType.INSTANCE)
                                        .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                                        .withRepeatElementExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION)
                                        .withRepeatTransitionStateName(runCommand1.getName())
                                        .build();

    assertThat(sm.getStates()).hasSize(7);
    assertThat(sm.getNextStates("starting", TransitionType.SUCCESS)).hasSize(1).containsOnly(expectedNewState1);
    assertThat(sm.getNextStates(expectedNewState1.getName(), TransitionType.REPEAT))
        .hasSize(1)
        .containsOnly(runCommand1);
    assertThat(sm.getNextStates("command1", TransitionType.SUCCESS)).isNull();

    RepeatState expectedNewState2 = aRepeatState()
                                        .withName("Repeat " + runCommand3.getName())
                                        .withRepeatElementType(ContextElementType.INSTANCE)
                                        .withExecutionStrategy(ExecutionStrategy.PARALLEL)
                                        .withRepeatElementExpression(InstanceExpressionProcessor.DEFAULT_EXPRESSION)
                                        .withRepeatTransitionStateName(runCommand3.getName())
                                        .build();

    assertThat(sm.getNextStates("command2", TransitionType.SUCCESS)).hasSize(1).containsOnly(expectedNewState2);

    assertThat(sm.getNextStates("command3", TransitionType.SUCCESS)).isNull();
  }

  /**
   * The Class Notifier.
   */
  static class Notifier implements Runnable {
    @Inject private WaitNotifyEngine waitNotifyEngine;
    private boolean shouldFail;
    private String name;
    private int duration;
    private String uuid;

    /**
     * Creates a new Notifier object.
     *
     * @param name     name of notifier.
     * @param uuid     the uuid
     * @param duration duration to sleep for.
     */
    Notifier(String name, String uuid, int duration) {
      this(name, uuid, duration, false);
    }

    /**
     * Instantiates a new notifier.
     *
     * @param name       the name
     * @param uuid       the uuid
     * @param duration   the duration
     * @param shouldFail the should fail
     */
    Notifier(String name, String uuid, int duration, boolean shouldFail) {
      this.name = name;
      this.uuid = uuid;
      this.duration = duration;
      this.shouldFail = shouldFail;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      logger.info("duration = " + duration);
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        logger.error("", e);
      }
      StaticMap.putValue(name, System.currentTimeMillis());
      if (shouldFail) {
        waitNotifyEngine.notify(uuid, aStringNotifyResponseData().withData("FAILURE").build());
      } else {
        waitNotifyEngine.notify(uuid, aStringNotifyResponseData().withData("SUCCESS").build());
      }
    }
  }

  /**
   * The Class StateSync.
   *
   * @author Rishi
   */
  public static class StateSync extends State {
    private boolean shouldFail;

    /**
     * Instantiates a new state synch.
     *
     * @param name the name
     */
    public StateSync(String name) {
      this(name, false);
    }

    /**
     * Instantiates a new state synch.
     *
     * @param name       the name
     * @param shouldFail the should fail
     */
    public StateSync(String name, boolean shouldFail) {
      super(name, StateType.HTTP.name());
      this.shouldFail = shouldFail;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      logger.info("Executing ..." + getClass());
      ExecutionResponse response = new ExecutionResponse();
      StateExecutionData stateExecutionData = new TestStateExecutionData(getName(), System.currentTimeMillis() + "");
      response.setStateExecutionData(stateExecutionData);
      StaticMap.putValue(getName(), System.currentTimeMillis());
      logger.info("stateExecutionData:" + stateExecutionData);
      if (shouldFail) {
        response.setExecutionStatus(ExecutionStatus.FAILED);
      }
      return response;
    }

    /**
     * Handle abort event.
     *
     * @param context the context
     */
    @Override
    public void handleAbortEvent(ExecutionContext context) {}

    @Override
    public int hashCode() {
      return Objects.hash(shouldFail);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final StateSync other = (StateSync) obj;
      return Objects.equals(this.shouldFail, other.shouldFail);
    }
  }

  /**
   * The Class StateAsync.
   *
   * @author Rishi
   */
  public static class StateAsync extends State {
    private boolean shouldFail;
    private boolean shouldThrowException;
    private int duration;

    @Inject private Injector injector;

    /**
     * Instantiates a new state asynch.
     *
     * @param name     the name
     * @param duration the duration
     */
    public StateAsync(String name, int duration) {
      this(name, duration, false);
    }

    /**
     * Instantiates a new state asynch.
     *
     * @param name       the name
     * @param duration   the duration
     * @param shouldFail the should fail
     */
    public StateAsync(String name, int duration, boolean shouldFail) {
      this(name, duration, shouldFail, false);
    }

    /**
     * Instantiates a new State async.
     *
     * @param name                 the name
     * @param duration             the duration
     * @param shouldFail           the should fail
     * @param shouldThrowException the should throw exception
     */
    public StateAsync(String name, int duration, boolean shouldFail, boolean shouldThrowException) {
      super(name, StateType.HTTP.name());
      this.duration = duration;
      this.shouldFail = shouldFail;
      this.shouldThrowException = shouldThrowException;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      String uuid = generateUuid();

      logger.info("Executing ..." + StateAsync.class.getName() + "..duration=" + duration + ", uuid=" + uuid);
      ExecutionResponse response = new ExecutionResponse();
      response.setAsync(true);
      List<String> correlationIds = new ArrayList<>();
      correlationIds.add(uuid);
      response.setCorrelationIds(correlationIds);
      if (shouldThrowException) {
        throw new RuntimeException("Exception for test");
      }
      Notifier notifier = new Notifier(getName(), uuid, duration, shouldFail);
      injector.injectMembers(notifier);
      ThreadPool.execute(notifier);
      return response;
    }

    /**
     * Handle abort event.
     *
     * @param context the context
     */
    @Override
    public void handleAbortEvent(ExecutionContext context) {}

    /* (non-Javadoc)
     * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
     */
    @Override
    public ExecutionResponse handleAsyncResponse(
        ExecutionContext context, Map<String, NotifyResponseData> responseMap) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      for (Object response : responseMap.values()) {
        if (!"SUCCESS".equals(((StringNotifyResponseData) response).getData())) {
          executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
        }
      }
      return executionResponse;
    }

    @Override
    public int hashCode() {
      return Objects.hash(shouldFail, shouldThrowException, duration, injector);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final StateAsync other = (StateAsync) obj;
      return Objects.equals(this.shouldFail, other.shouldFail)
          && Objects.equals(this.shouldThrowException, other.shouldThrowException)
          && Objects.equals(this.duration, other.duration) && Objects.equals(this.injector, other.injector);
    }
  }

  /**
   * The Class TestStateExecutionData.
   */
  public static class TestStateExecutionData extends StateExecutionData {
    private String key;
    private String value;

    /**
     * Instantiates a new test state execution data.
     */
    public TestStateExecutionData() {}

    /**
     * Instantiates a new test state execution data.
     *
     * @param key   the key
     * @param value the value
     */
    public TestStateExecutionData(String key, String value) {
      this.key = key;
      this.value = value;
    }

    /**
     * Gets key.
     *
     * @return the key
     */
    public String getKey() {
      return key;
    }

    /**
     * Sets key.
     *
     * @param key the key
     */
    public void setKey(String key) {
      this.key = key;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * Sets value.
     *
     * @param value the value
     */
    public void setValue(String value) {
      this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return "TestStateExecutionData [key=" + key + ", value=" + value + "]";
    }
  }
}
