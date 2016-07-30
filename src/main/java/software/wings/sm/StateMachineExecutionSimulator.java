/**
 *
 */
package software.wings.sm;

import software.wings.beans.EntityType;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.RepeatState;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Rishi
 *
 */
@Singleton
public class StateMachineExecutionSimulator {
  @Inject ExpressionProcessorFactory expressionProcessorFactory;

  public RequiredExecutionArgs getRequiredExecutionArgs(StateMachine stateMachine) {
    RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
    extrapolateRequiredExecutionArgs(
        stateMachine, stateMachine.getInitialState(), requiredExecutionArgs, new HashSet<>());
    return requiredExecutionArgs;
  }

  private void extrapolateRequiredExecutionArgs(StateMachine stateMachine, State state,
      RequiredExecutionArgs requiredExecutionArgs, Set<EntityType> argsInContext) {
    if (state == null) {
      return;
    }
    if (state instanceof RepeatState) {
      ExpressionProcessor processor =
          expressionProcessorFactory.getExpressionProcessor(((RepeatState) state).getRepeatElementExpression(), null);
      if (processor != null) {
        Set<EntityType> repeatArgsInContext = new HashSet<>(argsInContext);
        addArgsTypeFromContextElement(repeatArgsInContext, processor.getContextElementType());
        State repeat = stateMachine.getState(((RepeatState) state).getRepeatTransitionStateName());
        extrapolateRequiredExecutionArgs(stateMachine, repeat, requiredExecutionArgs, repeatArgsInContext);
      }
    }

    if (state.getRequiredExecutionArgumentTypes() != null) {
      for (EntityType type : state.getRequiredExecutionArgumentTypes()) {
        if (!argsInContext.contains(type)) {
          requiredExecutionArgs.getEntityTypes().add(type);
        }
      }
    }

    State success = stateMachine.getSuccessTransition(state.getName());
    if (success != null) {
      extrapolateRequiredExecutionArgs(stateMachine, success, requiredExecutionArgs, argsInContext);
    }

    State failure = stateMachine.getFailureTransition(state.getName());
    if (failure != null) {
      extrapolateRequiredExecutionArgs(stateMachine, failure, requiredExecutionArgs, argsInContext);
    }
    if (state instanceof CommandState) {
      requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_USER);
      requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_PASSWORD);
    }
  }

  /**
   * @param contextElementType
   * @return
   */
  private void addArgsTypeFromContextElement(Set<EntityType> argsInContext, ContextElementType contextElementType) {
    if (contextElementType == null) {
      return;
    }
    switch (contextElementType) {
      case SERVICE: {
        argsInContext.add(EntityType.SERVICE);
        return;
      }
      case INSTANCE: {
        argsInContext.add(EntityType.SERVICE);
        return;
      }
    }
  }
}
