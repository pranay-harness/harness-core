/**
 *
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.SUB_WORKFLOW;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Graph renderer.
 *
 * @author Rishi
 */
@Singleton
public class GraphRenderer {
  private static final Logger logger = LoggerFactory.getLogger(GraphRenderer.class);
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

  /**
   * Generate hierarchy node node.
   *
   * @param instanceIdMap    the instance id map
   * @param initialStateName the initial state name
   * @return the node
   */
  public GraphNode generateHierarchyNode(Map<String, StateExecutionInstance> instanceIdMap, String initialStateName) {
    logger.debug("generateSubworkflows request received - instanceIdMap: {}, initialStateName: {}", instanceIdMap,
        initialStateName);
    GraphNode originNode = null;
    Map<String, GraphNode> nodeIdMap = new HashMap<>();
    Map<String, GraphNode> prevInstanceIdMap = new HashMap<>();
    Map<String, Map<String, GraphNode>> parentIdElementsMap = new HashMap<>();

    for (StateExecutionInstance instance : instanceIdMap.values()) {
      GraphNode node = convertToNode(instance);

      if (node.getName().equals(initialStateName)) {
        originNode = node;
      }

      final String parentInstanceId = instance.getParentInstanceId();
      if (parentInstanceId != null && instance.isContextTransition()) {
        Map<String, GraphNode> elementsMap =
            parentIdElementsMap.computeIfAbsent(parentInstanceId, key -> new HashMap<>());

        if (isSubWorkflow(instanceIdMap.get(parentInstanceId))) {
          elementsMap.put(Constants.SUB_WORKFLOW, node);
          continue;
        }

        if (instance.getContextElement() == null) {
          continue;
        }
        elementsMap.put(instance.getContextElement().getName(), node);
      }

      if (instance.getPrevInstanceId() != null) {
        prevInstanceIdMap.put(instance.getPrevInstanceId(), node);
      }

      nodeIdMap.put(node.getId(), node);
    }
    logger.debug(
        "generateNodeTree invoked - instanceIdMap: {}, nodeIdMap: {}, prevInstanceIdMap: {}, parentIdElementsMap: {}, originNode: {}",
        instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode);

    generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode, null);

    // special treatment to avoid unnecessary hierarchy
    adjustProvisionNode(originNode);

    return originNode;
  }

  static boolean isProvisionNode(GraphNode node) {
    return PHASE_STEP.name().equals(node.getType()) && node.getName().equals(Constants.PROVISION_NODE_NAME)
        && node.getGroup() != null && isNotEmpty(node.getGroup().getElements());
  }

  static void adjustProvisionNode(GraphNode node) {
    if (node == null) {
      return;
    }

    adjustProvisionNode(node.getGroup());

    if (node.getNext() == null) {
      return;
    }

    GraphNode next = node.getNext();
    if (isProvisionNode(next)) {
      GraphNode nextToNext = next.getNext();
      GraphNode provisionStep = next.getGroup().getElements().get(0);
      node.setNext(provisionStep);
      provisionStep.setNext(nextToNext);
    }
    adjustProvisionNode(node.getNext());
  }

  static void adjustProvisionNode(GraphGroup group) {
    if (group == null || isEmpty(group.getElements())) {
      return;
    }

    GraphNode first = group.getElements().get(0);
    if (isProvisionNode(first)) {
      GraphNode nextToNext = first.getNext();
      GraphNode provisionStep = first.getGroup().getElements().get(0);
      provisionStep.setNext(nextToNext);
      group.getElements().set(0, provisionStep);
    }
    group.getElements().forEach(child -> adjustProvisionNode(child));
  }

  /**
   * Convert to node node.
   *
   * @param instance the instance
   * @return the node
   */
  GraphNode convertToNode(StateExecutionInstance instance) {
    GraphNode node = new GraphNode();
    node.setId(instance.getUuid());
    node.setName(instance.getStateName());
    node.setType(instance.getStateType());
    node.setRollback(instance.isRollback());
    node.setStatus(String.valueOf(instance.getStatus()).toUpperCase());

    if (instance.getStateExecutionDataHistory() != null) {
      node.setExecutionHistoryCount(instance.getStateExecutionDataHistory().size());
    }

    int interrupts = (int) workflowExecutionService.getExecutionInterruptCount(instance.getUuid());
    if (instance.getInterruptHistory() != null) {
      interrupts += instance.getInterruptHistory().size();
    }
    node.setInterruptHistoryCount(interrupts);

    if (instance.getStateExecutionData() != null) {
      StateExecutionData executionData = instance.getStateExecutionData();
      injector.injectMembers(executionData);
      try {
        node.setExecutionSummary(executionData.getExecutionSummary());
      } catch (Exception e) {
        logger.error("Failed to get state execution summary for state instance id {} and state name {}",
            instance.getUuid(), instance.getStateName(), e);
      }
      try {
        node.setExecutionDetails(executionData.getExecutionDetails());
      } catch (Exception e) {
        logger.error("Failed to get state execution details for state instance id {} and state name {}",
            instance.getUuid(), instance.getStateName(), e);
      }

      if (executionData instanceof ElementStateExecutionData) {
        ElementStateExecutionData elementStateExecutionData = (ElementStateExecutionData) executionData;
        try {
          node.setElementStatusSummary(elementStateExecutionData.getElementStatusSummary());
        } catch (Exception e) {
          logger.error("Failed to get state element status summary for state instance id {} and state name {}",
              instance.getUuid(), instance.getStateName(), e);
        }
      }
    }
    if (instance.getExecutionType() == WorkflowType.SIMPLE
        && StateType.COMMAND.name().equals(instance.getStateType())) {
      node.setName(((CommandStateExecutionData) instance.getStateExecutionData()).getCommandName());
    }
    if (instance.getStateParams() != null) {
      node.setProperties(instance.getStateParams());
    }
    return node;
  }

  private void generateNodeTree(Map<String, StateExecutionInstance> instanceIdMap, Map<String, GraphNode> nodeIdMap,
      Map<String, GraphNode> prevInstanceIdMap, Map<String, Map<String, GraphNode>> parentIdElementsMap, GraphNode node,
      StateExecutionData elementStateExecutionData) {
    logger.debug("generateNodeTree requested- node: {}", node);
    StateExecutionInstance instance = instanceIdMap.get(node.getId());

    if (elementStateExecutionData != null && elementStateExecutionData.getStartTs() == null) {
      elementStateExecutionData.setStartTs(instance.getStartTs());
    }

    if (parentIdElementsMap.get(node.getId()) != null) {
      GraphGroup group = new GraphGroup();
      group.setId(node.getId() + "-group");
      logger.debug("generateNodeTree group attached - group: {}, node: {}", group, node);
      node.setGroup(group);

      Collection<String> elements = null;
      StateExecutionData sed = instance.getStateExecutionData();
      if (sed != null && sed instanceof ForkStateExecutionData) {
        elements = ((ForkStateExecutionData) sed).getElements();
      } else if (sed != null && sed instanceof RepeatStateExecutionData) {
        elements = ((RepeatStateExecutionData) sed)
                       .getRepeatElements()
                       .stream()
                       .map(ContextElement::getName)
                       .collect(Collectors.toList());
        group.setExecutionStrategy(((RepeatStateExecutionData) sed).getExecutionStrategy());
      }

      logger.debug("generateNodeTree processing group - node: {}", elements);
      if (elements == null) {
        elements = parentIdElementsMap.get(node.getId()).keySet();
      }
      for (String element : elements) {
        generateElement(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, node, group, element);
      }
    }

    if (prevInstanceIdMap.get(node.getId()) != null) {
      GraphNode nextNode = prevInstanceIdMap.get(node.getId());
      logger.debug("generateNodeTree nextNode attached - nextNode: {}, node: {}", nextNode, node);
      node.setNext(nextNode);
      generateNodeTree(
          instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, nextNode, elementStateExecutionData);
    } else {
      if (elementStateExecutionData != null) {
        StateExecutionData executionData = instance.getStateExecutionData();
        if (executionData != null) {
          elementStateExecutionData.setStatus(executionData.getStatus());
          elementStateExecutionData.setEndTs(executionData.getEndTs());
          elementStateExecutionData.setErrorMsg(executionData.getErrorMsg());
        }
      }
    }
  }

  private void generateElement(Map<String, StateExecutionInstance> instanceIdMap, Map<String, GraphNode> nodeIdMap,
      Map<String, GraphNode> prevInstanceIdMap, Map<String, Map<String, GraphNode>> parentIdElementsMap, GraphNode node,
      GraphGroup group, String element) {
    if (element.equals(Constants.SUB_WORKFLOW)) {
      GraphNode elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
      if (elementRepeatNode != null) {
        group.getElements().add(elementRepeatNode);
        logger.debug("generateNodeTree elementRepeatNode added - node: {}", elementRepeatNode);
        generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode, null);
      }
      return;
    }

    GraphNode elementNode =
        aGraphNode().withId(node.getId() + "-" + element).withName(element).withType("ELEMENT").build();

    group.getElements().add(elementNode);
    logger.debug("generateNodeTree elementNode added - node: {}", elementNode);
    GraphNode elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
    StateExecutionData executionData = new StateExecutionData();
    if (elementRepeatNode != null) {
      elementNode.setNext(elementRepeatNode);
      logger.debug("generateNodeTree elementNode next added - node: {}", elementRepeatNode);
      generateNodeTree(
          instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode, executionData);
    }
    if (executionData.getStatus() == null) {
      executionData.setStatus(ExecutionStatus.QUEUED);
    }
    elementNode.setStatus(executionData.getStatus().name());
    elementNode.setExecutionSummary(executionData.getExecutionSummary());
    elementNode.setExecutionDetails(executionData.getExecutionDetails());
  }
}
