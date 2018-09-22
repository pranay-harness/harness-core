package software.wings.sm;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Created by rishi on 8/2/16.
 */
@Singleton
public class ExecutionContextFactory {
  @Inject private Injector injector;

  /**
   * Create execution context execution context.
   *
   * @param stateExecutionInstance the state execution instance
   * @param stateMachine           the state machine
   * @return the execution context
   */
  public ExecutionContext createExecutionContext(
      StateExecutionInstance stateExecutionInstance, StateMachine stateMachine) {
    return new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
  }
}
