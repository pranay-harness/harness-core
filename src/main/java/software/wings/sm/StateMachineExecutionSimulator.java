/**
 *
 */

package software.wings.sm;

import software.wings.beans.command.Command;
import software.wings.beans.EntityType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.RepeatState;
import software.wings.utils.JsonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The type State machine execution simulator.
 *
 * @author Rishi
 */
@Singleton
public class StateMachineExecutionSimulator {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutionContextFactory executionContextFactory;

  public RequiredExecutionArgs getRequiredExecutionArgs(
      String appId, String envId, StateMachine stateMachine, ExecutionArgs executionArgs) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    stateExecutionInstance.getContextElements().push(stdParams);
    ExecutionContextImpl context =
        (ExecutionContextImpl) executionContextFactory.createExecutionContext(stateExecutionInstance, stateMachine);

    RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
    extrapolateRequiredExecutionArgs(
        context, stateMachine, stateMachine.getInitialState(), requiredExecutionArgs, new HashSet<>());
    return requiredExecutionArgs;
  }

  private void extrapolateRequiredExecutionArgs(ExecutionContextImpl context, StateMachine stateMachine, State state,
      RequiredExecutionArgs requiredExecutionArgs, Set<EntityType> argsInContext) {
    if (state == null) {
      return;
    }
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    stateExecutionInstance.setStateName(state.getName());

    if (state instanceof RepeatState) {
      String repeatElementExpression = ((RepeatState) state).getRepeatElementExpression();
      List<ContextElement> repeatElements = (List<ContextElement>) context.evaluateExpression(repeatElementExpression);
      State repeat = stateMachine.getState(((RepeatState) state).getRepeatTransitionStateName());
      repeatElements.forEach(repeatElement -> {
        StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
        cloned.setStateName(repeat.getName());
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        childContext.pushContextElement(repeatElement);
        Set<EntityType> repeatArgsInContext = new HashSet<>(argsInContext);
        addArgsTypeFromContextElement(repeatArgsInContext, repeatElement.getElementType());
        extrapolateRequiredExecutionArgs(
            childContext, stateMachine, repeat, requiredExecutionArgs, repeatArgsInContext);
      });
    }

    if (state.getRequiredExecutionArgumentTypes() != null) {
      for (EntityType type : state.getRequiredExecutionArgumentTypes()) {
        if (argsInContext.contains(type)) {
          continue;
        }
        if (type == EntityType.INSTANCE) {
          requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_USER);
          requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_PASSWORD);
        } else {
          requiredExecutionArgs.getEntityTypes().add(type);
        }
      }
    }

    State success = stateMachine.getSuccessTransition(state.getName());
    if (success != null) {
      extrapolateRequiredExecutionArgs(context, stateMachine, success, requiredExecutionArgs, argsInContext);
    }

    State failure = stateMachine.getFailureTransition(state.getName());
    if (failure != null) {
      extrapolateRequiredExecutionArgs(context, stateMachine, failure, requiredExecutionArgs, argsInContext);
    }
    if (state instanceof CommandState) {
      ContextElement service = context.getContextElement(ContextElementType.SERVICE);
      Command command = serviceResourceService.getCommandByName(
          context.getApp().getUuid(), service.getUuid(), ((CommandState) state).getCommandName());
      if (command.isArtifactNeeded()) {
        requiredExecutionArgs.getEntityTypes().add(EntityType.ARTIFACT);
      }
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

  void setExecutionContextFactory(ExecutionContextFactory executionContextFactory) {
    this.executionContextFactory = executionContextFactory;
  }
}
