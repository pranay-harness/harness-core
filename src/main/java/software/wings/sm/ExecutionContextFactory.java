package software.wings.sm;

import com.google.inject.assistedinject.Assisted;

import javax.annotation.Nullable;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public interface ExecutionContextFactory {
  ExecutionContextImpl create(
      @Assisted StateExecutionInstance stateExecutionInstance, @Assisted @Nullable StateMachine stateMachine);
}
