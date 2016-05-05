package software.wings.sm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.junit.Test;
import software.wings.app.WingsBootstrap;
import software.wings.beans.ErrorConstants;
import software.wings.common.UUIDGenerator;
import software.wings.common.thread.ThreadPool;
import software.wings.exception.WingsException;
import software.wings.service.StaticMap;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;

public class StateMachineTest {
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
  public void shouldThowDupErrorCode() {
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

  static class Notifier implements Runnable {
    private String uuid;
    private int duration;

    /**
     * Creates a new Notifier object.
     *
     * @param uuid
     *          uuid of notifier.
     * @param duration
     *          duration to sleep for.
     */
    public Notifier(String uuid, int duration) {
      this.uuid = uuid;
      this.duration = duration;
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
      WingsBootstrap.lookup(WaitNotifyEngine.class).notify(uuid, "SUCCESS");
    }
  }

  /**
   * @author Rishi
   */
  public static class StateSynch extends State {
    public StateSynch(String name) {
      super(name, StateType.HTTP.name());
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
      return response;
    }
  }

  /**
   * @author Rishi
   */
  public static class StateAsynch extends State {
    private int duration;

    public StateAsynch(String name, int duration) {
      super(name, StateType.HTTP.name());
      this.duration = duration;
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
      ThreadPool.execute(new Notifier(uuid, duration));
      return response;
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
