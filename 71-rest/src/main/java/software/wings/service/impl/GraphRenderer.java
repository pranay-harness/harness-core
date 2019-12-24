package software.wings.service.impl;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.ABORTING;
import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.EXPIRED;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.PAUSING;
import static io.harness.beans.ExecutionStatus.REJECTED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.SUB_WORKFLOW;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.FeatureName;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.beans.GraphNode.GraphNodeBuilder;
import software.wings.common.Constants;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.sm.states.ForkState.ForkStateExecutionData;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

@Singleton
@Slf4j
public class GraphRenderer {
  public static final int AGGREGATION_LIMIT = 10;

  public static final long algorithmId = 0;

  @Inject private Injector injector;

  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;

  static boolean isSubWorkflow(StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance == null) {
      return false;
    }

    final String stateTypeName = stateExecutionInstance.getStateType();
    if (stateTypeName == null) {
      return false;
    }

    final StateType stateType = StateType.valueOf(stateTypeName);
    return stateType == SUB_WORKFLOW || stateType == PHASE_STEP || stateType == PHASE;
  }

  static ExecutionStatus aggregateStatus(List<ExecutionStatus> statuses) {
    if (isEmpty(statuses)) {
      return null;
    }

    List<ExecutionStatus> activeStatuses =
        statuses.stream().filter(status -> !ExecutionStatus.isFinalStatus(status)).collect(toList());

    for (ExecutionStatus status : asList(WAITING, PAUSED, PAUSING, RUNNING)) {
      if (activeStatuses.stream().anyMatch(active -> active == status)) {
        return status;
      }
    }
    final Optional<ExecutionStatus> notNewActiveStatuses =
        activeStatuses.stream().filter(status -> status != NEW).findFirst();
    if (notNewActiveStatuses.isPresent()) {
      return notNewActiveStatuses.get();
    }

    if (!activeStatuses.isEmpty()) {
      return NEW;
    }

    for (ExecutionStatus status : asList(REJECTED, EXPIRED, ABORTED, ABORTING, ERROR, FAILED)) {
      if (statuses.stream().anyMatch(active -> active == status)) {
        return status;
      }
    }

    return SUCCESS;
  }

  static ExecutionStatus aggregateDataStatus(ExecutionStatus current, List<StateExecutionData> executionDataList) {
    final List<ExecutionStatus> executionStatuses =
        executionDataList.stream().map(StateExecutionData::getStatus).collect(toList());
    if (current != null) {
      executionStatuses.add(0, current);
    }

    return aggregateStatus(executionStatuses);
  }

  static Long aggregateStartTs(Long current, List<StateExecutionData> executionDataList) {
    final Long startTs = executionDataList.stream()
                             .map(StateExecutionData::getStartTs)
                             .filter(Objects::nonNull)
                             .min(Long::compare)
                             .orElse(null);

    if (startTs == null) {
      return current;
    }
    if (current == null) {
      return startTs;
    }

    return current < startTs ? current : startTs;
  }

  static Long aggregateEndTs(Long current, List<StateExecutionData> executionDataList) {
    final Long endTs = executionDataList.stream()
                           .map(StateExecutionData::getEndTs)
                           .filter(Objects::nonNull)
                           .max(Long::compare)
                           .orElse(null);

    if (endTs == null) {
      return current;
    }
    if (current == null) {
      return endTs;
    }

    return current > endTs ? current : endTs;
  }

  static String aggregateErrorMessage(String current, List<StateExecutionData> executionDataList) {
    List<String> messages =
        executionDataList.stream().map(StateExecutionData::getErrorMsg).filter(Objects::nonNull).collect(toList());
    if (current != null) {
      messages.add(current);
    }

    messages = messages.stream().distinct().collect(toList());

    if (isEmpty(messages)) {
      return null;
    }

    if (messages.size() == 1) {
      return messages.get(0);
    }

    return "Multiple errors";
  }

  class Session {
    private boolean provideAggregatedNodes;
    private Map<String, StateExecutionInstance> instanceIdMap;

    private Map<String, StateExecutionInstance> prevInstanceIdMap = new HashMap<>();
    private Map<String, Map<String, StateExecutionInstance>> parentIdElementsMap = new HashMap<>();

    Session(boolean provideAggregatedNodes, Map<String, StateExecutionInstance> instanceIdMap) {
      this.provideAggregatedNodes = provideAggregatedNodes;
      this.instanceIdMap = instanceIdMap;
    }

    GraphNode generateNodeTree(List<StateExecutionInstance> instances, StateExecutionData elementStateExecutionData) {
      StateExecutionInstance instance = instances.get(0);
      GraphNode node = instances.size() == 1 ? convertToNode(instance) : aggregateToNode(instances);

      if (elementStateExecutionData != null) {
        final List<StateExecutionData> executionDataList = instances.stream()
                                                               .map(StateExecutionInstance::fetchStateExecutionData)
                                                               .filter(Objects::nonNull)
                                                               .collect(toList());

        elementStateExecutionData.setStatus(
            aggregateDataStatus(elementStateExecutionData.getStatus(), executionDataList));
        elementStateExecutionData.setStartTs(
            aggregateStartTs(elementStateExecutionData.getStartTs(), executionDataList));
        elementStateExecutionData.setEndTs(aggregateEndTs(elementStateExecutionData.getEndTs(), executionDataList));
        elementStateExecutionData.setErrorMsg(
            aggregateErrorMessage(elementStateExecutionData.getErrorMsg(), executionDataList));
      }

      if (parentIdElementsMap.get(instance.getUuid()) != null) {
        if (instances.size() > 1) {
          throw new WingsException("You need to start supporting aggregation of aggregations");
        }

        GraphGroup group = new GraphGroup();
        group.setId(node.getId() + "-group");
        logger.debug("generateNodeTree group attached - group: {}, node: {}", group, node);
        node.setGroup(group);

        Collection<String> elements = null;
        Collection<String> aggregateElements = null;
        StateExecutionData sed = instance.fetchStateExecutionData();
        if (sed instanceof ForkStateExecutionData) {
          elements = ((ForkStateExecutionData) sed).getElements();
        } else if (sed instanceof RepeatStateExecutionData) {
          String accountId = appService.getAccountIdByAppId(instance.getAppId());
          boolean aggregationEnabled = featureFlagService.isEnabled(FeatureName.NODE_AGGREGATION, accountId);
          group.setExecutionStrategy(((RepeatStateExecutionData) sed).getExecutionStrategy());

          Collection<String> repeatedElements = ((RepeatStateExecutionData) sed)
                                                    .getRepeatElements()
                                                    .stream()
                                                    .map(ContextElement::getName)
                                                    .collect(toList());

          if (!aggregationEnabled) {
            elements = repeatedElements;
            aggregateElements = null;
          } else {
            if (repeatedElements.size() < AGGREGATION_LIMIT) {
              elements = repeatedElements;
              aggregateElements = null;
            } else {
              elements = Collections.emptyList();
              aggregateElements = repeatedElements;
            }
          }
        }

        logger.debug("generateNodeTree processing group - node: {}", elements);
        if (elements == null) {
          elements = parentIdElementsMap.get(instance.getUuid()).keySet();
        }
        for (String element : elements) {
          generateElement(instance, group, element);
        }

        if (aggregateElements != null) {
          generateAggregateElement(
              instance, group, aggregateElements, !elements.isEmpty(), aggregateElements.size() + elements.size());
        }
      }

      final List<StateExecutionInstance> nextInstances = instances.stream()
                                                             .map(item -> prevInstanceIdMap.get(item.getUuid()))
                                                             .filter(Objects::nonNull)
                                                             .collect(toList());

      if (!nextInstances.isEmpty()) {
        if (nextInstances.size() != instances.size()) {
          throw new UnexpectedException();
        }
        GraphNode nextNode = generateNodeTree(nextInstances, elementStateExecutionData);
        node.setNext(nextNode);
      }

      return node;
    }

    void generateElement(StateExecutionInstance instance, GraphGroup group, String element) {
      if (element.equals(Constants.SUB_WORKFLOW)) {
        StateExecutionInstance elementRepeatInstance = parentIdElementsMap.get(instance.getUuid()).get(element);
        if (elementRepeatInstance != null) {
          final GraphNode elementRepeatNode = generateNodeTree(asList(elementRepeatInstance), null);
          group.getElements().add(elementRepeatNode);
        }
        return;
      }

      GraphNode elementNode =
          GraphNode.builder().id(instance.getUuid() + "-" + element).name(element).type("ELEMENT").build();

      group.getElements().add(elementNode);

      StateExecutionInstance elementRepeatInstance = parentIdElementsMap.get(instance.getUuid()).get(element);
      StateExecutionData executionData = new StateExecutionData();
      if (elementRepeatInstance != null) {
        final GraphNode elementRepeatNode = generateNodeTree(asList(elementRepeatInstance), executionData);
        elementNode.setNext(elementRepeatNode);
      }
      if (executionData.getStatus() == null) {
        executionData.setStatus(ExecutionStatus.QUEUED);
      }
      elementNode.setStatus(executionData.getStatus().name());
      elementNode.setExecutionSummary(executionData.getExecutionSummary());
      elementNode.setExecutionDetails(executionData.getExecutionDetails());
    }

    void generateAggregateElement(StateExecutionInstance instance, GraphGroup group, Collection<String> elements,
        boolean hasExpanded, int total) {
      String name = provideAggregatedNodes ? aggregateNodeName(provideAggregatedNodes, elements.size(), hasExpanded)
                                           : "" + total + " instances";

      GraphNode elementNode = GraphNode.builder()
                                  .id(instance.getUuid() + "-aggregate")
                                  .name(name)
                                  .type("ELEMENTS")
                                  .status(instance.getStatus().name())
                                  .build();

      group.getElements().add(elementNode);

      if (!provideAggregatedNodes || elements.isEmpty()) {
        StateExecutionData executionData = instance.fetchStateExecutionData();
        if (executionData.getStatus() == null) {
          executionData.setStatus(ExecutionStatus.QUEUED);
        }
        elementNode.setStatus(executionData.getStatus().name());

        elementNode.setExecutionDetails(executionData.getExecutionDetails());
        elementNode.setExecutionSummary(executionData.getExecutionSummary());
        return;
      }

      List<StateExecutionInstance> elementRepeatInstances =
          elements.stream().map(element -> parentIdElementsMap.get(instance.getUuid()).get(element)).collect(toList());

      StateExecutionData executionData = new StateExecutionData();
      final GraphNode elementRepeatNode = generateNodeTree(elementRepeatInstances, executionData);
      elementNode.setNext(elementRepeatNode);

      if (executionData.getStatus() == null) {
        executionData.setStatus(ExecutionStatus.QUEUED);
      }
      elementNode.setStatus(executionData.getStatus().name());

      elementNode.setExecutionDetails(executionData.getExecutionDetails());
      elementNode.setExecutionSummary(executionData.getExecutionSummary());
    }

    GraphNode generateHierarchyNode(boolean subGraph) {
      if (isEmpty(instanceIdMap)) {
        return null;
      }

      StateExecutionInstance origin = null;

      if (logger.isDebugEnabled()) {
        logger.debug("generateSubworkflows request received - instanceIdMap: {}", instanceIdMap);
      }

      for (StateExecutionInstance instance : instanceIdMap.values()) {
        if (subGraph) {
          if (instance.getPrevInstanceId() == null) {
            origin = instance;
          }
        } else {
          if (instance.getPrevInstanceId() == null && instance.getParentInstanceId() == null) {
            origin = instance;
          }
        }

        final String parentInstanceId = instance.getParentInstanceId();
        if (parentInstanceId != null && instance.isContextTransition()) {
          Map<String, StateExecutionInstance> elementsMap =
              parentIdElementsMap.computeIfAbsent(parentInstanceId, key -> new HashMap<>());

          if (isSubWorkflow(instanceIdMap.get(parentInstanceId))) {
            elementsMap.put(Constants.SUB_WORKFLOW, instance);
            continue;
          }

          if (instance.getContextElement() == null) {
            continue;
          }
          elementsMap.put(instance.getContextElement().getName(), instance);
        }

        if (instance.getPrevInstanceId() != null) {
          prevInstanceIdMap.put(instance.getPrevInstanceId(), instance);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("generateNodeTree invoked - instanceIdMap: {}, prevInstanceIdMap: {}, parentIdElementsMap: {}",
            instanceIdMap, prevInstanceIdMap, parentIdElementsMap);
      }

      return generateNodeTree(asList(origin), null);
    }

    public GraphNode generateElementNode(StateExecutionInstance repeatInstance, String element) {
      GraphNode elementNode =
          GraphNode.builder().id(repeatInstance.getUuid() + "-" + element).name(element).type("ELEMENT").build();

      StateExecutionData executionData = repeatInstance.fetchStateExecutionData();
      if (executionData.getStatus() == null) {
        executionData.setStatus(ExecutionStatus.QUEUED);
      }

      Map<String, ExecutionDataValue> executionDetails = executionData.getExecutionDetails();
      if (isNotEmpty(executionDetails)) {
        Map<String, ExecutionDataValue> elementExecutionData = new HashMap<>();
        elementExecutionData.put("startTs", executionDetails.get("startTs"));
        elementExecutionData.put("endTs", executionDetails.get("endTs"));
        elementNode.setExecutionDetails(elementExecutionData);
      }

      elementNode.setExecutionSummary(executionData.getExecutionSummary());
      elementNode.setStatus(executionData.getStatus().name());

      return elementNode;
    }
  }

  /**
   * Generate hierarchy node node.
   *
   * @param instanceIdMap    the instance id map
   * @return the node
   */
  public GraphNode generateHierarchyNode(Map<String, StateExecutionInstance> instanceIdMap) {
    final Session session = new Session(false, instanceIdMap);

    return session.generateHierarchyNode(false);
  }

  public GraphGroup generateNodeSubGraph(
      LinkedHashMap<String, Map<String, StateExecutionInstance>> elementIdToInstanceIdMap,
      StateExecutionInstance repeatInstance) {
    GraphGroup group = new GraphGroup();
    List<GraphNode> nodeSubGraph = new ArrayList<>();
    group.setElements(nodeSubGraph);
    for (Entry<String, Map<String, StateExecutionInstance>> hostElement : elementIdToInstanceIdMap.entrySet()) {
      Map<String, StateExecutionInstance> instanceMap = hostElement.getValue();
      Session session = new Session(false, instanceMap);
      GraphNode node = session.generateHierarchyNode(true);

      StateExecutionData sed = repeatInstance.fetchStateExecutionData();
      notNullCheck("stateExecutionData not populated for instance: " + repeatInstance.getUuid(), sed);
      if (!(sed instanceof RepeatStateExecutionData)) {
        throw new InvalidRequestException("Request for elements of instance that is not repeated", USER);
      }

      ContextElement element = ((RepeatStateExecutionData) sed)
                                   .getRepeatElements()
                                   .stream()
                                   .filter(contextElement -> contextElement.getUuid().equals(hostElement.getKey()))
                                   .findFirst()
                                   .orElse(null);

      GraphNode instanceElementNode = session.generateElementNode(repeatInstance, element.getName());
      instanceElementNode.setNext(node);
      nodeSubGraph.add(instanceElementNode);
    }
    return group;
  }

  GraphNode convertToNode(StateExecutionInstance instance) {
    GraphNodeBuilder builder = GraphNode.builder()
                                   .id(instance.getUuid())
                                   .name(instance.getDisplayName())
                                   .type(instance.getStateType())
                                   .rollback(instance.isRollback())
                                   .status(String.valueOf(instance.getStatus()).toUpperCase())
                                   .hasInspection(instance.isHasInspection());

    if (instance.getStateExecutionDataHistory() != null) {
      builder.executionHistoryCount(instance.getStateExecutionDataHistory().size());
    }
    int interrupts = instance.getDedicatedInterruptCount() == null
        ? workflowExecutionService.getExecutionInterruptCount(instance.getUuid())
        : instance.getDedicatedInterruptCount();

    if (instance.getInterruptHistory() != null) {
      interrupts += instance.getInterruptHistory().size();
    }
    builder.interruptHistoryCount(interrupts);

    if (instance.fetchStateExecutionData() != null) {
      StateExecutionData executionData = instance.fetchStateExecutionData();
      injector.injectMembers(executionData);
      try {
        builder.executionSummary(executionData.getExecutionSummary());
      } catch (RuntimeException e) {
        logger.error("Failed to get state execution summary for state instance id {} and state name {}",
            instance.getUuid(), instance.getDisplayName(), e);
      }
      try {
        builder.executionDetails(executionData.getExecutionDetails());
      } catch (RuntimeException e) {
        logger.error("Failed to get state execution details for state instance id {} and state name {}",
            instance.getUuid(), instance.getDisplayName(), e);
      }

      if (executionData instanceof ElementStateExecutionData) {
        ElementStateExecutionData elementStateExecutionData = (ElementStateExecutionData) executionData;
        try {
          builder.elementStatusSummary(elementStateExecutionData.getElementStatusSummary());
        } catch (RuntimeException e) {
          logger.error("Failed to get state element status summary for state instance id {} and state name {}",
              instance.getUuid(), instance.getDisplayName(), e);
        }
      }
    }
    if (instance.getStateParams() != null) {
      builder.properties(instance.getStateParams());
    }
    return builder.build();
  }

  GraphNode aggregateToNode(List<StateExecutionInstance> instances) {
    if (instances.size() <= 1) {
      throw new UnexpectedException();
    }

    StateExecutionInstance instance = instances.get(0);
    if (instances.stream().noneMatch(item -> instance.getDisplayName().equals(item.getDisplayName()))) {
      throw new UnexpectedException();
    }
    if (instances.stream().noneMatch(item -> instance.getStateType().equals(item.getStateType()))) {
      throw new UnexpectedException();
    }
    if (instances.stream().noneMatch(item -> instance.isRollback() == item.isRollback())) {
      throw new UnexpectedException();
    }

    ExecutionStatus status =
        aggregateStatus(instances.stream().map(StateExecutionInstance::getStatus).collect(toList()));

    final Multiset<ExecutionStatus> multiset = HashMultiset.create();
    instances.stream().map(StateExecutionInstance::getStatus).forEach(state -> multiset.add(state));

    Map<String, ExecutionDataValue> executionDetails = new LinkedHashMap<>();

    executionDetails.put(
        "Total instances", ExecutionDataValue.builder().displayName("Total instances").value(instances.size()).build());

    for (ExecutionStatus st : ExecutionStatus.values()) {
      int count = multiset.count(st);
      if (count > 0) {
        String displayName = executionStateDisplayName(st);

        executionDetails.put(displayName, ExecutionDataValue.builder().displayName(displayName).value(count).build());
      }
    }

    GraphNodeBuilder builder = GraphNode.builder()
                                   .id(generateUuid())
                                   .name(instance.getDisplayName())
                                   .type(instance.getStateType())
                                   .rollback(instance.isRollback())
                                   .status(String.valueOf(status).toUpperCase())
                                   .executionDetails(executionDetails);

    return builder.build();
  }

  static String executionStateDisplayName(ExecutionStatus status) {
    switch (status) {
      case NEW:
        return "New";
      case STARTING:
        return "About to start";
      case RUNNING:
        return "Still running";
      case SUCCESS:
        return "Succeeded";
      case DISCONTINUING:
        return "About to discontinue";
      case ABORTED:
        return "Aborted";
      case FAILED:
      case ERROR:
        return "Failed";
      case QUEUED:
        return "In the queue";
      case SCHEDULED:
        return "Scheduled";
      case WAITING:
        return "Waiting";
      case PAUSING:
        return "About to pause";
      case PAUSED:
        return "Paused";
      case RESUMED:
        return "Resumed";
      case REJECTED:
        return "Rejected";
      case EXPIRED:
        return "Expired";
      default:
        unhandled(status);
    }
    return "Other";
  }

  static String aggregateNodeName(boolean provideAggregatedNodes, int instanceCount, boolean hasExpanded) {
    if (instanceCount == 0) {
      return "instances";
    }
    if (provideAggregatedNodes) {
      return instanceCount + " instances";
    }

    if (hasExpanded) {
      return instanceCount + " more instances";
    }

    return instanceCount + " instances";
  }
}
