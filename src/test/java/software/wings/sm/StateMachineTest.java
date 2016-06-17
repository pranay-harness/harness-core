package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.google.inject.Inject;
import com.google.inject.Injector;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ErrorCodes;
import software.wings.common.UUIDGenerator;
import software.wings.common.thread.ThreadPool;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.RepeatState;
import software.wings.waitnotify.NotifyEventListener;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class StateMachineTest.
 */
@Listeners(NotifyEventListener.class)
public class StateMachineTest extends WingsBaseTest {
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
      assertThat(exception).hasMessage(ErrorCodes.DUPLICATE_STATE_NAMES.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.TRANSITION_TYPE_NULL.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.TRANSITION_NOT_LINKED.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.TRANSITION_TO_INCORRECT_STATE.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.STATES_WITH_DUP_TRANSITIONS.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.NON_FORK_STATES.getCode());
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
      assertThat(exception).hasMessage(ErrorCodes.NON_REPEAT_STATES.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("nonRepeatStates");
      assertThat(exception.getParams().get("nonRepeatStates")).asString().contains("StateB").contains("StateC");
    }
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
    public Notifier(String name, String uuid, int duration) {
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
    public Notifier(String name, String uuid, int duration, boolean shouldFail) {
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
      System.out.println("duration = " + duration);
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      StaticMap.putValue(name, System.currentTimeMillis());
      if (shouldFail) {
        waitNotifyEngine.notify(uuid, "FAILURE");
      } else {
        waitNotifyEngine.notify(uuid, "SUCCESS");
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
      System.out.println("Executing ..." + getClass());
      ExecutionResponse response = new ExecutionResponse();
      StateExecutionData stateExecutionData = new TestStateExecutionData(getName(), System.currentTimeMillis() + "");
      response.setStateExecutionData(stateExecutionData);
      StaticMap.putValue(getName(), System.currentTimeMillis());
      System.out.println("stateExecutionData:" + stateExecutionData);
      if (shouldFail) {
        response.setExecutionStatus(ExecutionStatus.FAILED);
      }
      return response;
    }
  }

  /**
   * The Class StateAsync.
   *
   * @author Rishi
   */
  public static class StateAsync extends State {
    private boolean shouldFail;
    private int duration;

    @Inject private Injector injector;
    private RuntimeException exception;

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
      super(name, StateType.HTTP.name());
      this.duration = duration;
      this.shouldFail = shouldFail;
    }

    /**
     * @param string
     * @param i
     * @param exception
     */
    public StateAsync(String name, int duration, RuntimeException exception) {
      super(name, StateType.HTTP.name());
      this.duration = duration;
      this.exception = exception;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      String uuid = UUIDGenerator.getUuid();

      System.out.println("Executing ..." + StateAsync.class.getName() + "..duration=" + duration + ", uuid=" + uuid);
      ExecutionResponse response = new ExecutionResponse();
      response.setAsync(true);
      List<String> correlationIds = new ArrayList<>();
      correlationIds.add(uuid);
      response.setCorrelationIds(correlationIds);
      if (exception != null) {
        throw exception;
      }
      Notifier notifier = new Notifier(getName(), uuid, duration, shouldFail);
      injector.injectMembers(notifier);
      ThreadPool.execute(notifier);
      return response;
    }

    /* (non-Javadoc)
     * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
     */
    @Override
    public ExecutionResponse handleAsyncResponse(
        ExecutionContextImpl context, Map<String, ? extends Serializable> responseMap) {
      ExecutionResponse executionResponse = new ExecutionResponse();
      for (Serializable response : responseMap.values()) {
        if (!"SUCCESS".equals(response)) {
          executionResponse.setExecutionStatus(ExecutionStatus.FAILED);
        }
      }
      return executionResponse;
    }
  }

  /**
   * The Class TestStateExecutionData.
   */
  public static class TestStateExecutionData extends StateExecutionData {
    private static final long serialVersionUID = -4839494609772157079L;
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
      super();
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
