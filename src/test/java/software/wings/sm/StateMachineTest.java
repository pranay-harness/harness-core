package software.wings.sm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.junit.Test;
import software.wings.app.WingsBootstrap;
import software.wings.beans.ErrorConstants;
import software.wings.common.UUIDGenerator;
import software.wings.common.thread.ThreadPool;
import software.wings.exception.WingsException;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;

public class StateMachineTest {
  @Test
  public void testValidate() {
    StateMachine sm = new StateMachine();
    State state = new StateA();
    sm.addState(state);
    state = new StateB();
    sm.addState(state);
    state = new StateC();
    sm.addState(state);
    sm.setInitialStateName(StateA.class.getName());
    assertThat(true).as("Validate result").isEqualTo(sm.validate());
  }

  @Test
  public void testValidateDup() {
    try {
      StateMachine sm = new StateMachine();
      State state = new StateA();
      sm.addState(state);
      state = new StateB();
      sm.addState(state);
      state = new StateC();
      sm.addState(state);
      sm.setInitialStateName(StateA.class.getName());
      state = new StateB();
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
  public static class StateA extends State {
    public StateA() {
      super(StateA.class.getName(), StateType.HTTP.name());
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      System.out.println("Executing ..." + getClass());
      context.setParam("StateA", StateA.class.getName());
      System.out.println("context params:" + context.getParams());
      return new ExecutionResponse();
    }
  }

  /**
   * @author Rishi
   */
  public static class StateAsynch extends State {
    private String name;
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

  /**
   * @author Rishi
   */
  public static class StateB extends State {
    public StateB() {
      super(StateB.class.getName(), StateType.HTTP.name());
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      context.setParam("StateB", StateB.class.getName());
      System.out.println("Executing ..." + getClass());
      System.out.println("context params:" + context.getParams());
      return new ExecutionResponse();
    }
  }

  /**
   * @author Rishi
   */
  public static class StateC extends State {
    public StateC() {
      super(StateC.class.getName(), StateType.HTTP.name());
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      context.setParam("StateC", StateC.class.getName());
      System.out.println("Executing ..." + getClass());
      System.out.println("context params:" + context.getParams());
      return new ExecutionResponse();
    }
  }
}
