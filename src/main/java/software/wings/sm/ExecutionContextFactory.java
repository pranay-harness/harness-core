package software.wings.sm;

import com.google.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by rishi on 8/2/16.
 */
@Singleton
public class ExecutionContextFactory {
  @Inject private Injector injector;

  public ExecutionContext createExecutionContext(
      StateExecutionInstance stateExecutionInstance, StateMachine stateMachine) {
    return new ExecutionContextImpl(stateExecutionInstance, stateMachine, injector);
  }
}
