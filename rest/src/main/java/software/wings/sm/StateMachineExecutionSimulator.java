/**
 *
 */

package software.wings.sm;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.ForkElement.Builder.aForkElement;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CountsByStatuses;
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
import software.wings.beans.infrastructure.ApplicationHost;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.RepeatState;
import software.wings.utils.JsonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  @Inject private HostService hostService;
  @Inject private SettingsService settingsService;

  /**
   * Gets status breakdown.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param stateMachine            the state machine
   * @param stateExecutionInstances the state execution instances
   * @return the status breakdown
   */
  public CountsByStatuses getStatusBreakdown(
      String appId, String envId, StateMachine stateMachine, List<StateExecutionInstance> stateExecutionInstances) {
    Map<String, StateExecutionInstance> stateExecutionInstanceMap =
        prepareStateExecutionInstanceMap(stateExecutionInstances);
    logger.debug("stateExecutionInstanceMap: {}", stateExecutionInstanceMap);
    ExecutionContextImpl context = getInitialExecutionContext(appId, envId, stateMachine);
    CountsByStatuses countsByStatuses = new CountsByStatuses();
    extrapolateProgress(
        countsByStatuses, context, stateMachine, stateMachine.getInitialState(), "", stateExecutionInstanceMap, false);
    return countsByStatuses;
  }

  /**
   * Gets required execution args.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param stateMachine  the state machine
   * @param executionArgs the execution args
   * @return the required execution args
   */
  public RequiredExecutionArgs getRequiredExecutionArgs(
      String appId, String envId, StateMachine stateMachine, ExecutionArgs executionArgs) {
    ExecutionContextImpl context = getInitialExecutionContext(appId, envId, stateMachine);

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

  private ExecutionContextImpl getInitialExecutionContext(String appId, String envId, StateMachine stateMachine) {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    stateExecutionInstance.getContextElements().push(stdParams);
    return (ExecutionContextImpl) executionContextFactory.createExecutionContext(stateExecutionInstance, stateMachine);
  }

  /**
   * Gets infrastructure required entity type.
   *
   * @param appId              the app id
   * @param serviceInstanceIds the service instance ids
   * @return the infrastructure required entity type
   */
  public Set<EntityType> getInfrastructureRequiredEntityType(String appId, Collection<String> serviceInstanceIds) {
    PageRequest<ServiceInstance> pageRequest = aPageRequest()
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
    List<ApplicationHost> hostResponse = hostService
                                             .list(aPageRequest()
                                                       .addFilter(ID_KEY, Operator.IN,
                                                           res.getResponse()
                                                               .stream()
                                                               .map(ServiceInstance::getHostId)
                                                               .collect(Collectors.toSet())
                                                               .toArray())
                                                       .build())
                                             .getResponse();

    for (ApplicationHost host : hostResponse) {
      SettingAttribute connAttribute = settingsService.get(host.getHost().getHostConnAttr());
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
        case KEY: {
          entityTypes.add(EntityType.SSH_KEY_PASSPHRASE);
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
      ContextElement repeatElement = repeatElements.get(0);

      // Now repeat for one element
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

    } else if (state instanceof ForkState) {
      ((ForkState) state).getForkStateNames().forEach(childStateName -> {
        State child = stateMachine.getState(childStateName);
        StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
        cloned.setStateName(child.getName());
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        cloned.setContextElement(
            aForkElement().withStateName(childStateName).withParentId(stateExecutionInstance.getUuid()).build());
        Set<EntityType> repeatArgsInContext = new HashSet<>(argsInContext);
        Set<EntityType> nextReqEntities = extrapolateRequiredExecutionArgs(
            childContext, stateMachine, child, repeatArgsInContext, commandMap, serviceInstanceIds);
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

  private void extrapolateProgress(CountsByStatuses countsByStatuses, ExecutionContextImpl context,
      StateMachine stateMachine, State state, String parentPath,
      Map<String, StateExecutionInstance> stateExecutionInstanceMap, boolean previousInprogress) {
    if (state == null) {
      return;
    }
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    stateExecutionInstance.setStateName(state.getName());

    String path = getKeyName(parentPath, stateExecutionInstance);

    if (state instanceof RepeatState) {
      String repeatElementExpression = ((RepeatState) state).getRepeatElementExpression();
      List<ContextElement> repeatElements = (List<ContextElement>) context.evaluateExpression(repeatElementExpression);
      if (repeatElements == null || repeatElements.isEmpty()) {
        logger.warn("No repeatElements found for the expression: {}", repeatElementExpression);
        return;
      }
      State repeat = stateMachine.getState(((RepeatState) state).getRepeatTransitionStateName());
      repeatElements.forEach(repeatElement -> {
        StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
        cloned.setStateName(repeat.getName());
        cloned.setContextElement(repeatElement);
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        childContext.pushContextElement(repeatElement);
        extrapolateProgress(
            countsByStatuses, childContext, stateMachine, repeat, path, stateExecutionInstanceMap, previousInprogress);
      });
    } else if (state instanceof ForkState) {
      ((ForkState) state).getForkStateNames().forEach(childStateName -> {
        State child = stateMachine.getState(childStateName);
        StateExecutionInstance cloned = JsonUtils.clone(stateExecutionInstance, StateExecutionInstance.class);
        cloned.setStateName(child.getName());
        cloned.setContextElement(
            aForkElement().withStateName(childStateName).withParentId(stateExecutionInstance.getUuid()).build());
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        extrapolateProgress(
            countsByStatuses, childContext, stateMachine, child, path, stateExecutionInstanceMap, previousInprogress);
      });
    } else {
      if (previousInprogress) {
        countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
        State success = stateMachine.getSuccessTransition(state.getName());
        extrapolateProgress(
            countsByStatuses, context, stateMachine, success, parentPath, stateExecutionInstanceMap, true);
      } else {
        ExecutionStatus status = getExecutionStatus(stateExecutionInstanceMap, path);
        if (status == ExecutionStatus.SUCCESS) {
          countsByStatuses.setSuccess(countsByStatuses.getSuccess() + 1);
          State success = stateMachine.getSuccessTransition(state.getName());
          extrapolateProgress(
              countsByStatuses, context, stateMachine, success, parentPath, stateExecutionInstanceMap, false);
        } else if (status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR
            || status == ExecutionStatus.ABORTING || status == ExecutionStatus.ABORTED) {
          countsByStatuses.setFailed(countsByStatuses.getFailed() + 1);
          State failed = stateMachine.getFailureTransition(state.getName());
          extrapolateProgress(
              countsByStatuses, context, stateMachine, failed, parentPath, stateExecutionInstanceMap, false);
        } else if (status == ExecutionStatus.RUNNING || status == ExecutionStatus.STARTING
            || status == ExecutionStatus.NEW) {
          countsByStatuses.setInprogress(countsByStatuses.getInprogress() + 1);
          State success = stateMachine.getSuccessTransition(state.getName());
          extrapolateProgress(
              countsByStatuses, context, stateMachine, success, parentPath, stateExecutionInstanceMap, true);
        } else {
          countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
          State success = stateMachine.getSuccessTransition(state.getName());
          extrapolateProgress(
              countsByStatuses, context, stateMachine, success, parentPath, stateExecutionInstanceMap, true);
        }
      }
    }
  }

  private ExecutionStatus getExecutionStatus(
      Map<String, StateExecutionInstance> stateExecutionInstanceMap, String path) {
    StateExecutionInstance stateExecutionInstance = stateExecutionInstanceMap.get(path);
    if (stateExecutionInstance != null) {
      return stateExecutionInstance.getStatus();
    }
    return null;
  }

  private Map<String, StateExecutionInstance> prepareStateExecutionInstanceMap(
      List<StateExecutionInstance> stateExecutionInstances) {
    Map<String, StateExecutionInstance> stateExecutionInstanceMap = new HashMap<>();
    if (stateExecutionInstances == null || stateExecutionInstances.isEmpty()) {
      return stateExecutionInstanceMap;
    }

    Map<String, StateExecutionInstance> stateExecutionInstanceIdMap = stateExecutionInstances.stream().collect(
        Collectors.toMap(StateExecutionInstance::getUuid, Function.identity()));

    stateExecutionInstances.forEach(stateExecutionInstance -> {
      stateExecutionInstanceMap.put(
          getKeyName(stateExecutionInstanceIdMap, stateExecutionInstance), stateExecutionInstance);
    });

    return stateExecutionInstanceMap;
  }

  private String getKeyName(
      Map<String, StateExecutionInstance> stateExecutionInstanceIdMap, StateExecutionInstance stateExecutionInstance) {
    String parentPath = "";
    if (stateExecutionInstance.getParentInstanceId() != null) {
      parentPath = getKeyName(
          stateExecutionInstanceIdMap, stateExecutionInstanceIdMap.get(stateExecutionInstance.getParentInstanceId()));
    }
    return getKeyName(parentPath, stateExecutionInstance);
  }

  private String getKeyName(String parentPath, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getContextElement() == null
        || StringUtils.isBlank(stateExecutionInstance.getContextElement().getName())) {
      return parentPath + "__" + stateExecutionInstance.getStateName();
    } else {
      return parentPath + "__" + stateExecutionInstance.getContextElement().getName() + "__"
          + stateExecutionInstance.getStateName();
    }
  }

  private boolean isArtifactNeeded(ExecutionContextImpl context, CommandState state, Map<String, Command> commandMap) {
    ContextElement service = context.getContextElement(ContextElementType.SERVICE);
    Command command = commandMap.get(state.getName());
    if (command == null) {
      command = serviceResourceService
                    .getCommandByName(context.getApp().getUuid(), service.getUuid(), context.getEnv().getUuid(),
                        ((CommandState) state).getCommandName())
                    .getCommand();
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

  /**
   * Sets execution context factory.
   *
   * @param executionContextFactory the execution context factory
   */
  void setExecutionContextFactory(ExecutionContextFactory executionContextFactory) {
    this.executionContextFactory = executionContextFactory;
  }
}
