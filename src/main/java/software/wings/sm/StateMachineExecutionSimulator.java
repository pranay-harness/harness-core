/**
 *
 */

package software.wings.sm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.Command;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.RepeatState;
import software.wings.utils.JsonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutionContextFactory executionContextFactory;
  @Inject private ServiceInstanceService serviceInstanceService;

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
    Set<String> serviceInstanceIds = new HashSet<>();
    Set<EntityType> entityTypes = extrapolateRequiredExecutionArgs(
        context, stateMachine, stateMachine.getInitialState(), new HashSet<>(), new HashMap<>(), serviceInstanceIds);
    if (serviceInstanceIds.size() > 0) {
      Set<EntityType> infraEntityTypes = getInfrastructureRequiredEntityType(appId, serviceInstanceIds);
      entityTypes.remove(EntityType.INSTANCE);
      entityTypes.addAll(infraEntityTypes);
    }
    requiredExecutionArgs.setEntityTypes(entityTypes);
    return requiredExecutionArgs;
  }

  public Set<EntityType> getInfrastructureRequiredEntityType(String appId, Collection<String> serviceInstanceIds) {
    PageRequest<ServiceInstance> pageRequest = PageRequest.Builder.aPageRequest()
                                                   .addFilter("appId", Operator.EQ, appId)
                                                   .addFilter("uuid", Operator.IN, serviceInstanceIds.toArray())
                                                   .addFieldsIncluded("uuid", "host")
                                                   .build();

    PageResponse<ServiceInstance> res = serviceInstanceService.list(pageRequest);
    if (res == null || res.isEmpty()) {
      logger.error("No service instance found for the ids: {}", serviceInstanceIds);
      throw new WingsException(ErrorCodes.DEFAULT_ERROR_CODE);
    }

    Set<AccessType> accessTypes = new HashSet<>();
    for (ServiceInstance serviceInstance : res.getResponse()) {
      SettingAttribute connAttribute = serviceInstance.getHost().getHostConnAttr();
      if (connAttribute == null || connAttribute.getValue() == null
          || !(connAttribute.getValue() instanceof HostConnectionAttributes)
          || ((HostConnectionAttributes) connAttribute.getValue()).getAccessType() == null) {
        continue;
      }
      accessTypes.add(((HostConnectionAttributes) connAttribute.getValue()).getAccessType());
    }

    Set<EntityType> entityTypes = new HashSet<>();
    accessTypes.forEach(accessType -> {
      switch (accessType) {
        case USER_PASSWORD: {
          entityTypes.add(EntityType.SSH_USER);
          entityTypes.add(EntityType.SSH_PASSWORD);
          break;
        }
        case USER_PASSWORD_SU_APP_USER: {
          entityTypes.add(EntityType.SSH_USER);
          entityTypes.add(EntityType.SSH_PASSWORD);
          entityTypes.add(EntityType.SSH_APP_ACCOUNT);
          entityTypes.add(EntityType.SSH_APP_ACCOUNT_PASSOWRD);
          break;
        }
        case USER_PASSWORD_SUDO_APP_USER: {
          entityTypes.add(EntityType.SSH_USER);
          entityTypes.add(EntityType.SSH_PASSWORD);
          entityTypes.add(EntityType.SSH_APP_ACCOUNT);
          break;
        }
      }
    });

    return entityTypes;
  }

  private Set<EntityType> extrapolateRequiredExecutionArgs(ExecutionContextImpl context, StateMachine stateMachine,
      State state, Set<EntityType> argsInContext, Map<String, Command> commandMap, Set<String> serviceInstanceIds) {
    if (state == null) {
      return null;
    }
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    stateExecutionInstance.setStateName(state.getName());

    Set<EntityType> entityTypes = new HashSet<>();

    if (state instanceof RepeatState) {
      String repeatElementExpression = ((RepeatState) state).getRepeatElementExpression();
      List<ContextElement> repeatElements = (List<ContextElement>) context.evaluateExpression(repeatElementExpression);
      if (repeatElements == null || repeatElements.isEmpty()) {
        logger.warn("No repeatElements found for the expression: {}", repeatElementExpression);
        return null;
      }
      State repeat = stateMachine.getState(((RepeatState) state).getRepeatTransitionStateName());
      repeatElements.forEach(repeatElement -> {
        StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
        cloned.setStateName(repeat.getName());
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        childContext.pushContextElement(repeatElement);
        Set<EntityType> repeatArgsInContext = new HashSet<>(argsInContext);
        addArgsTypeFromContextElement(repeatArgsInContext, repeatElement.getElementType());
        Set<EntityType> nextReqEntities = extrapolateRequiredExecutionArgs(
            childContext, stateMachine, repeat, repeatArgsInContext, commandMap, serviceInstanceIds);
        if (nextReqEntities != null) {
          entityTypes.addAll(nextReqEntities);
        }
      });
    } else {
      if (state.getRequiredExecutionArgumentTypes() != null) {
        for (EntityType type : state.getRequiredExecutionArgumentTypes()) {
          if (type == EntityType.INSTANCE) {
            serviceInstanceIds.add(context.getContextElement(ContextElementType.INSTANCE).getUuid());
          }
          if (argsInContext.contains(type)) {
            continue;
          }
          entityTypes.add(type);
        }
      }
      if (state instanceof CommandState && isArtifactNeeded(context, (CommandState) state, commandMap)) {
        entityTypes.add(EntityType.ARTIFACT);
      }
    }

    State success = stateMachine.getSuccessTransition(state.getName());
    if (success != null) {
      Set<EntityType> nextReqEntities = extrapolateRequiredExecutionArgs(
          context, stateMachine, success, argsInContext, commandMap, serviceInstanceIds);
      if (nextReqEntities != null) {
        entityTypes.addAll(nextReqEntities);
      }
    }

    State failure = stateMachine.getFailureTransition(state.getName());
    if (failure != null) {
      Set<EntityType> nextReqEntities = extrapolateRequiredExecutionArgs(
          context, stateMachine, failure, argsInContext, commandMap, serviceInstanceIds);
      if (nextReqEntities != null) {
        entityTypes.addAll(nextReqEntities);
      }
    }
    return entityTypes;
  }

  private boolean isArtifactNeeded(ExecutionContextImpl context, CommandState state, Map<String, Command> commandMap) {
    ContextElement service = context.getContextElement(ContextElementType.SERVICE);
    Command command = commandMap.get(state.getName());
    if (command == null) {
      command = serviceResourceService.getCommandByName(
          context.getApp().getUuid(), service.getUuid(), ((CommandState) state).getCommandName());
      commandMap.put(state.getName(), command);
    }
    return command.isArtifactNeeded();
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
