/**
 *
 */

package software.wings.service.impl;

import static software.wings.beans.Graph.Node.Builder.aNode;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.beans.Graph.Group;
import software.wings.beans.Graph.Node;
import software.wings.beans.WorkflowType;
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

/**
 * The type Graph renderer.
 *
 * @author Rishi
 */
@Singleton
public class GraphRenderer {
  private static final Logger logger = LoggerFactory.getLogger(GraphRenderer.class);
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
  public Node generateHierarchyNode(Map<String, StateExecutionInstance> instanceIdMap, String initialStateName,
      List<String> expandedGroupIds, Boolean expandLastOnly, boolean allExpanded) {
    logger.debug(
        "generateSubworkflows request received - instanceIdMap: {}, initialStateName: {}, expandedGroupIds: {}",
        instanceIdMap, initialStateName, expandedGroupIds);
    Node originNode = null;
    Map<String, Node> nodeIdMap = new HashMap<>();
    Map<String, Node> prevInstanceIdMap = new HashMap<>();
    Map<String, Map<String, Node>> parentIdElementsMap = new HashMap<>();

    for (StateExecutionInstance instance : instanceIdMap.values()) {
      Node node = convertToNode(instance);

      if ((StateType.REPEAT.name().equals(instance.getStateType())
              || StateType.FORK.name().equals(instance.getStateType())
              || StateType.SUB_WORKFLOW.name().equals(instance.getStateType())
              || StateType.PHASE_STEP.name().equals(instance.getStateType())
              || StateType.PHASE.name().equals(instance.getStateType()))
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
            && (instanceIdMap.get(instance.getParentInstanceId()).getStateType().equals(StateType.SUB_WORKFLOW.name())
                   || instanceIdMap.get(instance.getParentInstanceId())
                          .getStateType()
                          .equals(StateType.PHASE_STEP.name())
                   || instanceIdMap.get(instance.getParentInstanceId())
                          .getStateType()
                          .equals(StateType.PHASE.name()))) {
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

    generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode, null, expandLastOnly,
        allExpanded);

    // special treatment to avoid unnecessary hierarchy
    adjustProvisionNode(originNode);

    return originNode;
  }

  private void adjustProvisionNode(Node node) {
    if (node == null) {
      return;
    }

    adjustProvisionNode(node.getGroup());

    if (node.getNext() != null) {
      Node next = node.getNext();
      if (StateType.PHASE_STEP.name().equals(next.getType()) && next.getName().equals(Constants.PROVISION_NODE_NAME)
          && next.getGroup() != null && next.getGroup().getElements() != null
          && !next.getGroup().getElements().isEmpty()) {
        Node nextToNext = next.getNext();
        Node provisionStep = next.getGroup().getElements().get(0);
        node.setNext(provisionStep);
        provisionStep.setNext(nextToNext);
      }
    }
    adjustProvisionNode(node.getNext());
  }

  private void adjustProvisionNode(Group group) {
    if (group == null || group.getElements() == null || group.getElements().isEmpty()) {
      return;
    }

    Node first = group.getElements().get(0);
    if (StateType.PHASE_STEP.name().equals(first.getType()) && first.getName().equals(Constants.PROVISION_NODE_NAME)
        && first.getGroup() != null && first.getGroup().getElements() != null
        && !first.getGroup().getElements().isEmpty()) {
      Node nextToNext = first.getNext();
      Node provisionStep = first.getGroup().getElements().get(0);
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
  Node convertToNode(StateExecutionInstance instance) {
    Node node = new Node();
    node.setId(instance.getUuid());
    node.setName(instance.getStateName());
    node.setType(instance.getStateType());
    node.setRollback(instance.isRollback());
    node.setStatus(String.valueOf(instance.getStatus()).toUpperCase());
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

  private void generateNodeTree(Map<String, StateExecutionInstance> instanceIdMap, Map<String, Node> nodeIdMap,
      Map<String, Node> prevInstanceIdMap, Map<String, Map<String, Node>> parentIdElementsMap, Node node,
      StateExecutionData elementStateExecutionData, Boolean expandLastOnly, boolean allExpanded) {
    logger.debug("generateNodeTree requested- node: {}", node);
    StateExecutionInstance instance = instanceIdMap.get(node.getId());

    if (elementStateExecutionData != null && elementStateExecutionData.getStartTs() == null) {
      elementStateExecutionData.setStartTs(instance.getStartTs());
    }

    if ((allExpanded || expandLastOnly == null || expandLastOnly) && parentIdElementsMap.get(node.getId()) != null) {
      Group group = new Group();
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
      logger.debug("generateNodeTree nextNode attached - nextNode: {}, node: {}", nextNode, node);
      node.setNext(nextNode);
      generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, nextNode,
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
        logger.debug("generateNodeTree elementRepeatNode added - node: {}", elementRepeatNode);
        generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode, null,
            expandLastOnly, allExpanded);
      }
      return;
    }

    Node elementNode = aNode().withId(node.getId() + "-" + element).withName(element).withType("ELEMENT").build();

    group.getElements().add(elementNode);
    logger.debug("generateNodeTree elementNode added - node: {}", elementNode);
    Node elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
    StateExecutionData executionData = new StateExecutionData();
    if (elementRepeatNode != null) {
      elementNode.setNext(elementRepeatNode);
      logger.debug("generateNodeTree elementNode next added - node: {}", elementRepeatNode);
      generateNodeTree(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode,
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
