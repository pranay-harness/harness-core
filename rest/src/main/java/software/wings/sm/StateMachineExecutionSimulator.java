/**
 *
 */

package software.wings.sm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.ForkElement.Builder.aForkElement;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.EntityType;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.RepeatState;
import software.wings.utils.KryoUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * The type State machine execution simulator.
 *
 * @author Rishi
 */
@Singleton
public class StateMachineExecutionSimulator {
  private static final Logger logger = LoggerFactory.getLogger(StateMachineExecutionSimulator.class);

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
      String appId, String envId, StateMachine stateMachine, Map<String, ExecutionStatus> stateExecutionStatuses) {
    ExecutionContextImpl context = getInitialExecutionContext(appId, envId, stateMachine);
    CountsByStatuses countsByStatuses = new CountsByStatuses();
    extrapolateProgress(
        countsByStatuses, context, stateMachine, stateMachine.getInitialState(), "", stateExecutionStatuses, false);
    return countsByStatuses;
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
                                                   .addFieldsIncluded("uuid", "hostId")
                                                   .build();

    PageResponse<ServiceInstance> res = serviceInstanceService.list(pageRequest);
    if (isEmpty(res)) {
      logger.error("No service instance found for the ids: {}", serviceInstanceIds);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE);
    }

    Set<AccessType> accessTypes = new HashSet<>();
    List<Host> hostResponse = hostService
                                  .list(aPageRequest()
                                            .addFilter(ID_KEY, Operator.IN,
                                                res.getResponse()
                                                    .stream()
                                                    .map(ServiceInstance::getHostId)
                                                    .collect(Collectors.toSet())
                                                    .toArray())
                                            .build())
                                  .getResponse();

    for (Host host : hostResponse) {
      SettingAttribute connAttribute = settingsService.get(host.getHostConnAttr());
      if (connAttribute == null || connAttribute.getValue() == null
          || !(connAttribute.getValue() instanceof HostConnectionAttributes)
          || ((HostConnectionAttributes) connAttribute.getValue()).getAccessType() == null) {
        continue;
      }
      accessTypes.add(((HostConnectionAttributes) connAttribute.getValue()).getAccessType());
    }

    Set<EntityType> entityTypes = new HashSet<>();
    accessTypes.forEach(accessType -> { populateRequiredEntityTypesByAccessType(entityTypes, accessType); });

    return entityTypes;
  }

  public static void populateRequiredEntityTypesByAccessType(Set<EntityType> entityTypes, AccessType accessType) {
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
      default:
        unhandled(accessType);
    }
  }

  private void extrapolateProgress(CountsByStatuses countsByStatuses, ExecutionContextImpl context,
      StateMachine stateMachine, State state, String parentPath, Map<String, ExecutionStatus> stateExecutionStatuses,
      boolean previousInprogress) {
    if (state == null) {
      return;
    }
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    stateExecutionInstance.setStateName(state.getName());
    stateExecutionInstance.setDisplayName(state.getName());

    String path = getKeyName(parentPath, stateExecutionInstance);

    if (state instanceof RepeatState) {
      String repeatElementExpression = ((RepeatState) state).getRepeatElementExpression();
      List<ContextElement> repeatElements = (List<ContextElement>) context.evaluateExpression(repeatElementExpression);
      if (isEmpty(repeatElements)) {
        logger.warn("No repeatElements found for the expression: {}", repeatElementExpression);
        return;
      }
      State repeat = stateMachine.getState(null, ((RepeatState) state).getRepeatTransitionStateName());
      repeatElements.forEach(repeatElement -> {
        StateExecutionInstance cloned = KryoUtils.clone(stateExecutionInstance);
        cloned.setStateParams(null);
        cloned.setDisplayName(repeat.getName());
        cloned.setContextElement(repeatElement);
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        childContext.pushContextElement(repeatElement);
        extrapolateProgress(
            countsByStatuses, childContext, stateMachine, repeat, path, stateExecutionStatuses, previousInprogress);
      });
      return;
    }

    if (state instanceof ForkState) {
      ((ForkState) state).getForkStateNames().forEach(childStateName -> {
        State child = stateMachine.getState(null, childStateName);
        StateExecutionInstance cloned = KryoUtils.clone(stateExecutionInstance);
        cloned.setStateParams(null);
        cloned.setDisplayName(child.getName());
        cloned.setContextElement(
            aForkElement().withStateName(childStateName).withParentId(stateExecutionInstance.getUuid()).build());
        ExecutionContextImpl childContext =
            (ExecutionContextImpl) executionContextFactory.createExecutionContext(cloned, stateMachine);
        extrapolateProgress(
            countsByStatuses, childContext, stateMachine, child, path, stateExecutionStatuses, previousInprogress);
      });
      return;
    }

    if (previousInprogress) {
      countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
      State success = stateMachine.getSuccessTransition(state.getName());
      extrapolateProgress(countsByStatuses, context, stateMachine, success, parentPath, stateExecutionStatuses, true);
      return;
    }

    ExecutionStatus status = getExecutionStatus(stateExecutionStatuses, path);
    if (status == ExecutionStatus.SUCCESS) {
      countsByStatuses.setSuccess(countsByStatuses.getSuccess() + 1);
      State success = stateMachine.getSuccessTransition(state.getName());
      extrapolateProgress(countsByStatuses, context, stateMachine, success, parentPath, stateExecutionStatuses, false);
      return;
    }
    if (ExecutionStatus.isNegativeStatus(status)) {
      countsByStatuses.setFailed(countsByStatuses.getFailed() + 1);
      State failed = stateMachine.getFailureTransition(state.getName());
      extrapolateProgress(countsByStatuses, context, stateMachine, failed, parentPath, stateExecutionStatuses, false);
      return;
    }
    if (status == ExecutionStatus.RUNNING || status == ExecutionStatus.STARTING || status == ExecutionStatus.NEW) {
      countsByStatuses.setInprogress(countsByStatuses.getInprogress() + 1);
      State success = stateMachine.getSuccessTransition(state.getName());
      extrapolateProgress(countsByStatuses, context, stateMachine, success, parentPath, stateExecutionStatuses, true);
      return;
    }

    countsByStatuses.setQueued(countsByStatuses.getQueued() + 1);
    State success = stateMachine.getSuccessTransition(state.getName());
    extrapolateProgress(countsByStatuses, context, stateMachine, success, parentPath, stateExecutionStatuses, true);
  }

  private ExecutionStatus getExecutionStatus(Map<String, ExecutionStatus> stateExecutionInstanceMap, String path) {
    return stateExecutionInstanceMap.get(path);
  }

  public void prepareStateExecutionInstanceMap(Iterator<StateExecutionInstance> stateExecutionInstances,
      Map<String, ExecutionStatus> stateExecutionInstanceMap) {
    // TODO: avoid remembering the instance objects
    Map<String, StateExecutionInstance> stateExecutionInstanceIdMap = new HashMap<>();
    while (stateExecutionInstances.hasNext()) {
      StateExecutionInstance stateExecutionInstance = stateExecutionInstances.next();
      stateExecutionInstanceIdMap.put(stateExecutionInstance.getUuid(), stateExecutionInstance);
    }

    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstanceIdMap.values()) {
      stateExecutionInstanceMap.put(
          getKeyName(stateExecutionInstanceIdMap, stateExecutionInstance), stateExecutionInstance.getStatus());
    }
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
        || isBlank(stateExecutionInstance.getContextElement().getName())) {
      return parentPath + "__" + stateExecutionInstance.getDisplayName();
    } else {
      return parentPath + "__" + stateExecutionInstance.getContextElement().getName() + "__"
          + stateExecutionInstance.getDisplayName();
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
