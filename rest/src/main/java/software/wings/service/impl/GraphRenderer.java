/**
 *
 */

package software.wings.service.impl;

import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Graph.Group;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The type Graph renderer.
 *
 * @author Rishi
 */
@Singleton
public class GraphRenderer {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private Injector injector;

  /**
   * Generate hierarchy node node.
   *
   * @param instanceIdMap    the instance id map
   * @param initialStateName the initial state name
   * @param expandedGroupIds the expanded group ids
   * @param expandLastOnly   the expand last only
   * @param allExpanded      the all expanded
   * @return the node
   */
  Node generateHierarchyNode(Map<String, StateExecutionInstance> instanceIdMap, String initialStateName,
      List<String> expandedGroupIds, Boolean expandLastOnly, boolean allExpanded) {
    logger.debug("generateGraph request received - instanceIdMap: {}, initialStateName: {}, expandedGroupIds: {}",
        instanceIdMap, initialStateName, expandedGroupIds);
    Node originNode = null;
    Map<String, Node> nodeIdMap = new HashMap<>();
    Map<String, Node> prevInstanceIdMap = new HashMap<>();
    Map<String, Map<String, Node>> parentIdElementsMap = new HashMap<>();

    for (StateExecutionInstance instance : instanceIdMap.values()) {
      Node node = convertToNode(instance);

      if ((StateType.REPEAT.name().equals(instance.getStateType())
              || StateType.FORK.name().equals(instance.getStateType())
              || StateType.SUB_WORKFLOW.name().equals(instance.getStateType()))
          && (allExpanded || expandedGroupIds == null || !expandedGroupIds.contains(instance.getUuid()))) {
        node.setExpanded(false);
      } else {
        node.setExpanded(true);
      }

      if (node.getName().equals(initialStateName)) {
        originNode = node;
      }

      if (instance.getParentInstanceId() != null && instance.isContextTransition()) {
        Map<String, Node> elementsMap = parentIdElementsMap.get(instance.getParentInstanceId());
        if (elementsMap == null) {
          elementsMap = new HashMap<>();
          parentIdElementsMap.put(instance.getParentInstanceId(), elementsMap);
        }
        if (instanceIdMap.get(instance.getParentInstanceId()) != null
            && instanceIdMap.get(instance.getParentInstanceId()).getStateType() != null
            && instanceIdMap.get(instance.getParentInstanceId()).getStateType().equals(Constants.SUB_WORKFLOW)) {
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
        "generateNodeHierarchy invoked - instanceIdMap: {}, nodeIdMap: {}, prevInstanceIdMap: {}, parentIdElementsMap: {}, originNode: {}",
        instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode);

    generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode, null,
        expandLastOnly, allExpanded);

    return originNode;
  }

  /**
   * Convert to node node.
   *
   * @param instance the instance
   * @return the node
   */
  Node convertToNode(StateExecutionInstance instance) {
    Node node = new Node();
    node.setId(instance.getUuid());
    node.setName(instance.getStateName());
    node.setType(instance.getStateType());
    node.setStatus(String.valueOf(instance.getStatus()).toUpperCase());
    if (instance.getStateExecutionData() != null) {
      StateExecutionData executionData = instance.getStateExecutionData();
      injector.injectMembers(executionData);
      node.setExecutionSummary(executionData.getExecutionSummary());
      node.setExecutionDetails(executionData.getExecutionDetails());
      if (executionData instanceof ElementStateExecutionData) {
        ElementStateExecutionData elementStateExecutionData = (ElementStateExecutionData) executionData;
        node.setElementStatusSummary(elementStateExecutionData.getElementStatusSummary());
      }
    }
    return node;
  }

  private void replaceCommandNodeName(List<Node> nodes, String commandName) {
    if (nodes != null) {
      nodes.forEach(node -> {
        if (StateType.COMMAND.name().equals(node.getType())) {
          node.setName(commandName);
        }
      });
    }
  }

  private void generateNodeHierarchy(Map<String, StateExecutionInstance> instanceIdMap, Map<String, Node> nodeIdMap,
      Map<String, Node> prevInstanceIdMap, Map<String, Map<String, Node>> parentIdElementsMap, Node node,
      StateExecutionData elementStateExecutionData, Boolean expandLastOnly, boolean allExpanded) {
    logger.debug("generateNodeHierarchy requested- node: {}", node);
    StateExecutionInstance instance = instanceIdMap.get(node.getId());

    if (elementStateExecutionData != null && elementStateExecutionData.getStartTs() == null) {
      elementStateExecutionData.setStartTs(instance.getStartTs());
    }

    if ((allExpanded || expandLastOnly == null || expandLastOnly) && parentIdElementsMap.get(node.getId()) != null) {
      Group group = new Group();
      group.setId(node.getId() + "-group");
      logger.debug("generateNodeHierarchy group attached - group: {}, node: {}", group, node);
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

      logger.debug("generateNodeHierarchy processing group - node: {}", elements);
      if (elements == null) {
        elements = parentIdElementsMap.get(node.getId()).keySet();
      }
      int i = 0;
      for (String element : elements) {
        generateElement(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, node,
            expandLastOnly == null ? null : (i == elements.size() - 1), allExpanded, group, element);
        i++;
      }
    }
    if (!allExpanded && expandLastOnly != null && !expandLastOnly) {
      node.setExpanded(false);
    }

    if (prevInstanceIdMap.get(node.getId()) != null) {
      Node nextNode = prevInstanceIdMap.get(node.getId());
      logger.debug("generateNodeHierarchy nextNode attached - nextNode: {}, node: {}", nextNode, node);
      node.setNext(nextNode);
      generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, nextNode,
          elementStateExecutionData, expandLastOnly, allExpanded);
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

  private void generateElement(Map<String, StateExecutionInstance> instanceIdMap, Map<String, Node> nodeIdMap,
      Map<String, Node> prevInstanceIdMap, Map<String, Map<String, Node>> parentIdElementsMap, Node node,
      Boolean expandLastOnly, boolean allExpanded, Group group, String element) {
    if (element.equals(Constants.SUB_WORKFLOW)) {
      Node elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
      if (elementRepeatNode != null) {
        group.getElements().add(elementRepeatNode);
        logger.debug("generateNodeHierarchy elementRepeatNode added - node: {}", elementRepeatNode);
        generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode, null,
            expandLastOnly, allExpanded);
      }
      return;
    }

    Node elementNode =
        Node.Builder.aNode().withId(node.getId() + "-" + element).withName(element).withType("ELEMENT").build();
    group.getElements().add(elementNode);
    logger.debug("generateNodeHierarchy elementNode added - node: {}", elementNode);
    Node elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
    StateExecutionData executionData = new StateExecutionData();
    if (elementRepeatNode != null) {
      elementNode.setNext(elementRepeatNode);
      logger.debug("generateNodeHierarchy elementNode next added - node: {}", elementRepeatNode);
      generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode,
          executionData, expandLastOnly, allExpanded);
    }
    if (executionData.getStatus() == null) {
      executionData.setStatus(ExecutionStatus.QUEUED);
    }
    elementNode.setStatus(executionData.getStatus().name());
    elementNode.setExecutionSummary(executionData.getExecutionSummary());
    elementNode.setExecutionDetails(executionData.getExecutionDetails());
  }
}
