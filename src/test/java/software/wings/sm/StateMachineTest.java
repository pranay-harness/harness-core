package software.wings.sm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.app.WingsBootstrap;
import software.wings.beans.ErrorConstants;
import software.wings.common.UUIDGenerator;
import software.wings.common.thread.ThreadPool;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.waitnotify.NotifyEventListener;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Listeners(NotifyEventListener.class)
public class StateMachineTest extends WingsBaseTest {
  @Test
  public void shouldValidate() {
    StateMachine sm = new StateMachine();
    State state = new StateSynch("StateA");
    sm.addState(state);
    state = new StateSynch("StateB");
    sm.addState(state);
    state = new StateSynch("StateC");
    sm.addState(state);
    sm.setInitialStateName("StateA");
    assertThat(true).as("Validate result").isEqualTo(sm.validate());
  }

  @Test
  public void shouldThrowDupErrorCode() {
    try {
      StateMachine sm = new StateMachine();
      State state = new StateSynch("StateA");
      sm.addState(state);
      state = new StateSynch("StateB");
      sm.addState(state);
      state = new StateSynch("StateC");
      sm.addState(state);
      sm.setInitialStateName("StateA");
      state = new StateSynch("StateB");
      sm.addState(state);
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorConstants.DUPLICATE_STATE_NAMES);
    }
  }

  @Test
  public void shouldThrowNullTransition() {
    try {
      StateMachine sm = new StateMachine();
      State stateA = new StateSynch("StateA");
      sm.addState(stateA);
      StateSynch stateB = new StateSynch("StateB");
      sm.addState(stateB);
      sm.setInitialStateName("StateA");

      sm.addTransition(Transition.Builder.aTransition().withToState(stateA).withFromState(stateB).build());
      sm.validate();
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorConstants.TRANSITION_TYPE_NULL);
    }
  }

  static class Notifier implements Runnable {
    private boolean shouldFail;
    private String name;
    private int duration;
    private String uuid;

    /**
     * Creates a new Notifier object.
     *
     * @param name     name of notifier.
     * @param duration duration to sleep for.
     */
    public Notifier(String name, String uuid, int duration) {
      this(name, uuid, duration, false);
    }
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
        WingsBootstrap.lookup(WaitNotifyEngine.class).notify(uuid, "FAILURE");
      } else {
        WingsBootstrap.lookup(WaitNotifyEngine.class).notify(uuid, "SUCCESS");
      }
    }
  }

  /**
   * @author Rishi
   */
  public static class StateSynch extends State {
    private boolean shouldFail;

    public StateSynch(String name) {
      this(name, false);
    }

    public StateSynch(String name, boolean shouldFail) {
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
   * @author Rishi
   */
  public static class StateAsynch extends State {
    private boolean shouldFail;
    private int duration;

    public StateAsynch(String name, int duration) {
      this(name, duration, false);
    }
    public StateAsynch(String name, int duration, boolean shouldFail) {
      super(name, StateType.HTTP.name());
      this.duration = duration;
      this.shouldFail = shouldFail;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      String uuid = UUIDGenerator.getUuid();

      System.out.println("Executing ..." + StateAsynch.class.getName() + "..duration=" + duration + ", uuid=" + uuid);
      ExecutionResponse response = new ExecutionResponse();
      response.setAsynch(true);
      List<String> correlationIds = new ArrayList<>();
      correlationIds.add(uuid);
      response.setCorrelationIds(correlationIds);
      ThreadPool.execute(new Notifier(getName(), uuid, duration, shouldFail));
      return response;
    }
    @Override
    public ExecutionResponse handleAsynchResponse(
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

  public static class TestStateExecutionData extends StateExecutionData {
    private static final long serialVersionUID = -4839494609772157079L;
    private String key;
    private String value;

    public TestStateExecutionData() {}

    public TestStateExecutionData(String key, String value) {
      super();
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "TestStateExecutionData [key=" + key + ", value=" + value + "]";
    }
  }
}
