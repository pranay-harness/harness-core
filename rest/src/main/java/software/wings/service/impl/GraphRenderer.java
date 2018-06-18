package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ABORTING;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.EXPIRED;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.REJECTED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.SUB_WORKFLOW;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ExecutionDataValue;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.beans.GraphNode.GraphNodeBuilder;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.exception.UnexpectedException;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.sm.states.ForkState.ForkStateExecutionData;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class GraphRenderer {
  private static final Logger logger = LoggerFactory.getLogger(GraphRenderer.class);
  private static final int AGGREGATION_LIMIT = 10;

  @Inject private Injector injector;

  @Inject private WorkflowExecutionService workflowExecutionService;

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
      if (activeStatuses.stream().anyMatch(active -> active == status)) {
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
    private Set<String> excludeFromAggregation = new HashSet<>();
    private Map<String, StateExecutionInstance> instanceIdMap;

    private Map<String, StateExecutionInstance> prevInstanceIdMap = new HashMap<>();
    private Map<String, Map<String, StateExecutionInstance>> parentIdElementsMap = new HashMap<>();

    Session(boolean provideAggregatedNodes, Map<String, StateExecutionInstance> instanceIdMap,
        Set<String> excludeFromAggregation) {
      this.provideAggregatedNodes = provideAggregatedNodes;
      this.instanceIdMap = instanceIdMap;
      this.excludeFromAggregation = excludeFromAggregation;
    }

    GraphNode generateNodeTree(List<StateExecutionInstance> instances, StateExecutionData elementStateExecutionData) {
      StateExecutionInstance instance = instances.get(0);
      GraphNode node = instances.size() == 1 ? convertToNode(instance) : aggregateToNode(instances);

      if (elementStateExecutionData != null) {
        final List<StateExecutionData> executionDataList = instances.stream()
                                                               .map(StateExecutionInstance::getStateExecutionData)
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
        StateExecutionData sed = instance.getStateExecutionData();
        if (sed instanceof ForkStateExecutionData) {
          elements = ((ForkStateExecutionData) sed).getElements();
        } else if (sed instanceof RepeatStateExecutionData) {
          group.setExecutionStrategy(((RepeatStateExecutionData) sed).getExecutionStrategy());

          Collection<String> repeatedElements = ((RepeatStateExecutionData) sed)
                                                    .getRepeatElements()
                                                    .stream()
                                                    .map(ContextElement::getName)
                                                    .collect(toList());

          if (isEmpty(excludeFromAggregation) && repeatedElements.size() < AGGREGATION_LIMIT) {
            elements = repeatedElements;
            aggregateElements = null;
          } else {
            final Map<Boolean, List<String>> split =
                repeatedElements.stream().collect(Collectors.partitioningBy(element -> {
                  final StateExecutionInstance item = parentIdElementsMap.get(instance.getUuid()).get(element);
                  if (item == null) {
                    return false;
                  }
                  return !excludeFromAggregation.contains(item.getContextElement().getUuid());
                }));
            aggregateElements = split.get(true);
            elements = split.get(false);
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
          aGraphNode().withId(instance.getUuid() + "-" + element).withName(element).withType("ELEMENT").build();

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

      GraphNode elementNode = aGraphNode()
                                  .withId(instance.getUuid() + "-aggregate")
                                  .withName(name)
                                  .withType("ELEMENTS")
                                  .withStatus(instance.getStatus().name())
                                  .build();

      group.getElements().add(elementNode);

      if (!provideAggregatedNodes || elements.isEmpty()) {
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
    }

    GraphNode generateHierarchyNode() {
      StateExecutionInstance origin = null;

      if (logger.isDebugEnabled()) {
        logger.debug("generateSubworkflows request received - instanceIdMap: {}", instanceIdMap);
      }

      for (StateExecutionInstance instance : instanceIdMap.values()) {
        if (instance.getPrevInstanceId() == null && instance.getParentInstanceId() == null) {
          origin = instance;
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
  }

  /**
   * Generate hierarchy node node.
   *
   * @param instanceIdMap    the instance id map
   * @param initialStateName the initial state name
   * @return the node
   */
  public GraphNode generateHierarchyNode(
      Map<String, StateExecutionInstance> instanceIdMap, Set<String> excludeFromAggregation) {
    final Session session = new Session(false, instanceIdMap, excludeFromAggregation);

    GraphNode node = session.generateHierarchyNode();

    // special treatment to avoid unnecessary hierarchy
    adjustInfrastructureNode(node);

    return node;
  }

  GraphNode convertToNode(StateExecutionInstance instance) {
    GraphNodeBuilder builder = aGraphNode()
                                   .withId(instance.getUuid())
                                   .withName(instance.getDisplayName())
                                   .withType(instance.getStateType())
                                   .withRollback(instance.isRollback())
                                   .withStatus(String.valueOf(instance.getStatus()).toUpperCase());

    if (instance.getStateExecutionDataHistory() != null) {
      builder.withExecutionHistoryCount(instance.getStateExecutionDataHistory().size());
    }
    int interrupts = instance.getDedicatedInterruptCount() == null
        ? workflowExecutionService.getExecutionInterruptCount(instance.getUuid())
        : instance.getDedicatedInterruptCount();

    if (instance.getInterruptHistory() != null) {
      interrupts += instance.getInterruptHistory().size();
    }
    builder.withInterruptHistoryCount(interrupts);

    if (instance.getStateExecutionData() != null) {
      StateExecutionData executionData = instance.getStateExecutionData();
      injector.injectMembers(executionData);
      try {
        builder.withExecutionSummary(executionData.getExecutionSummary());
      } catch (RuntimeException e) {
        logger.error("Failed to get state execution summary for state instance id {} and state name {}",
            instance.getUuid(), instance.getDisplayName(), e);
      }
      try {
        builder.withExecutionDetails(executionData.getExecutionDetails());
      } catch (RuntimeException e) {
        logger.error("Failed to get state execution details for state instance id {} and state name {}",
            instance.getUuid(), instance.getDisplayName(), e);
      }

      if (executionData instanceof ElementStateExecutionData) {
        ElementStateExecutionData elementStateExecutionData = (ElementStateExecutionData) executionData;
        try {
          builder.withElementStatusSummary(elementStateExecutionData.getElementStatusSummary());
        } catch (RuntimeException e) {
          logger.error("Failed to get state element status summary for state instance id {} and state name {}",
              instance.getUuid(), instance.getDisplayName(), e);
        }
      }
    }
    if (instance.getExecutionType() == WorkflowType.SIMPLE
        && StateType.COMMAND.name().equals(instance.getStateType())) {
      builder.withName(((CommandStateExecutionData) instance.getStateExecutionData()).getCommandName());
    }
    if (instance.getStateParams() != null) {
      builder.withProperties(instance.getStateParams());
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

    GraphNodeBuilder builder = aGraphNode()
                                   .withId(generateUuid())
                                   .withName(instance.getDisplayName())
                                   .withType(instance.getStateType())
                                   .withRollback(instance.isRollback())
                                   .withStatus(String.valueOf(status).toUpperCase())
                                   .withExecutionDetails(executionDetails);

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

  static boolean isInfrastructureNode(GraphNode node) {
    return node != null && PHASE_STEP.name().equals(node.getType())
        && (node.getName().equals(Constants.INFRASTRUCTURE_NODE_NAME)
               || node.getName().equals(Constants.PROVISION_NODE_NAME))
        && node.getGroup() != null && isNotEmpty(node.getGroup().getElements());
  }

  static void adjustInfrastructureNode(GraphNode node) {
    if (node == null) {
      return;
    }

    adjustInfrastructureNode(node.getGroup());

    if (node.getNext() == null) {
      return;
    }

    GraphNode next = node.getNext();
    if (isInfrastructureNode(next)) {
      GraphNode nextToNext = next.getNext();
      GraphNode provisionStep = next.getGroup().getElements().get(0);
      node.setNext(provisionStep);
      provisionStep.setNext(nextToNext);
    }
    adjustInfrastructureNode(node.getNext());
  }

  static void adjustInfrastructureNode(GraphGroup group) {
    if (group == null || isEmpty(group.getElements())) {
      return;
    }

    GraphNode first = group.getElements().get(0);
    if (isInfrastructureNode(first)) {
      GraphNode nextToNext = first.getNext();
      GraphNode provisionStep = first.getGroup().getElements().get(0);
      provisionStep.setNext(nextToNext);
      group.getElements().set(0, provisionStep);
    }
    group.getElements().forEach(GraphRenderer::adjustInfrastructureNode);
  }
}
