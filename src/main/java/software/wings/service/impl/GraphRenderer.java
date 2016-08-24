/**
 *
 */

package software.wings.service.impl;

import static software.wings.beans.Graph.DEFAULT_ARROW_HEIGHT;
import static software.wings.beans.Graph.DEFAULT_ARROW_WIDTH;
import static software.wings.beans.Graph.DEFAULT_ELEMENT_NODE_WIDTH;
import static software.wings.beans.Graph.DEFAULT_ELEMENT_PADDING;
import static software.wings.beans.Graph.DEFAULT_GROUP_PADDING;
import static software.wings.beans.Graph.DEFAULT_INITIAL_X;
import static software.wings.beans.Graph.DEFAULT_INITIAL_Y;
import static software.wings.beans.Graph.DEFAULT_NODE_HEIGHT;
import static software.wings.beans.Graph.DEFAULT_NODE_WIDTH;

import com.google.inject.Injector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Group;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.common.UUIDGenerator;
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
   * Generate graph graph.
   *
   * @param instanceIdMap    the instance id map
   * @param initialStateName the initial state name
   * @param expandedGroupIds the expanded group ids
   * @param commandName      the command name
   * @param expandLastOnly   the expand last only
   * @return the graph
   */
  Graph generateGraph(Map<String, StateExecutionInstance> instanceIdMap, String initialStateName,
      List<String> expandedGroupIds, String commandName, Boolean expandLastOnly) {
    logger.debug("generateGraph request received - instanceIdMap: {}, initialStateName: {}, expandedGroupIds: {}",
        instanceIdMap, initialStateName, expandedGroupIds);
    Node originNode = null;
    Map<String, Node> nodeIdMap = new HashMap<>();
    Map<String, Node> prevInstanceIdMap = new HashMap<>();
    Map<String, Map<String, Node>> parentIdElementsMap = new HashMap<>();

    for (StateExecutionInstance instance : instanceIdMap.values()) {
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

      if ((StateType.REPEAT.name().equals(instance.getStateType())
              || StateType.FORK.name().equals(instance.getStateType()))
          && (expandedGroupIds == null || !expandedGroupIds.contains(instance.getUuid()))) {
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

    generateNodeHierarchy(
        instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, originNode, null, expandLastOnly);

    Graph graph = new Graph();
    extrapolateDimension(originNode);
    paintGraph(originNode, graph, DEFAULT_INITIAL_X, DEFAULT_INITIAL_Y);
    if (StringUtils.isNotBlank(commandName)) {
      replaceCommandNodeName(graph.getNodes(), commandName);
    }
    logger.debug("graph generated: {}", graph);
    return graph;
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
      StateExecutionData elementStateExecutionData, Boolean expandLastOnly) {
    logger.debug("generateNodeHierarchy requested- node: {}", node);
    StateExecutionInstance instance = instanceIdMap.get(node.getId());

    if (elementStateExecutionData != null && elementStateExecutionData.getStartTs() == null) {
      elementStateExecutionData.setStartTs(instance.getStartTs());
    }

    if ((expandLastOnly == null || expandLastOnly) && parentIdElementsMap.get(node.getId()) != null) {
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
            expandLastOnly == null ? null : (i == elements.size() - 1), group, element);
        i++;
      }
    }
    if (expandLastOnly != null && !expandLastOnly) {
      node.setExpanded(false);
    }

    if (prevInstanceIdMap.get(node.getId()) != null) {
      Node nextNode = prevInstanceIdMap.get(node.getId());
      logger.debug("generateNodeHierarchy nextNode attached - nextNode: {}, node: {}", nextNode, node);
      node.setNext(nextNode);
      generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, nextNode,
          elementStateExecutionData, expandLastOnly);
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
      Boolean expandLastOnly, Group group, String element) {
    Node elementNode =
        Node.Builder.aNode().withId(UUIDGenerator.getUuid()).withName(element).withType("ELEMENT").build();
    group.getElements().add(elementNode);
    logger.debug("generateNodeHierarchy elementNode added - node: {}", elementNode);
    Node elementRepeatNode = parentIdElementsMap.get(node.getId()).get(element);
    StateExecutionData executionData = new StateExecutionData();
    if (elementRepeatNode != null) {
      elementNode.setNext(elementRepeatNode);
      logger.debug("generateNodeHierarchy elementNode next added - node: {}", elementRepeatNode);
      generateNodeHierarchy(instanceIdMap, nodeIdMap, prevInstanceIdMap, parentIdElementsMap, elementRepeatNode,
          executionData, expandLastOnly);
    }
    if (executionData.getStatus() == null) {
      executionData.setStatus(ExecutionStatus.QUEUED);
    }
    elementNode.setStatus(executionData.getStatus().name());
    elementNode.setExecutionSummary(executionData.getExecutionSummary());
    elementNode.setExecutionDetails(executionData.getExecutionDetails());
  }

  private void paintGraph(Group group, Graph graph, int x, int y) {
    group.setX(x);
    group.setY(y);
    graph.addNode(group);

    y += DEFAULT_ARROW_HEIGHT;
    Node priorElement = null;
    for (Node node : group.getElements()) {
      paintGraph(node, graph, x + DEFAULT_ELEMENT_PADDING, y);
      y += node.getHeight() + DEFAULT_ARROW_HEIGHT;
      priorElement = node;
    }
    if (priorElement == null) {
      group.setHeight(0);
    } else {
      group.setHeight(priorElement.getY() + DEFAULT_NODE_HEIGHT / 2 - group.getY());
    }
  }

  private void paintGraph(Node node, Graph graph, int x, int y) {
    node.setX(x);
    node.setY(y);
    graph.addNode(node);

    Group group = node.getGroup();
    if (group != null) {
      paintGraph(group, graph, x + DEFAULT_GROUP_PADDING, y + DEFAULT_NODE_HEIGHT);
    }

    Node next = node.getNext();
    if (next != null) {
      if (group == null || next.getGroup() == null) {
        if (node.getType().equals("ELEMENT")) {
          paintGraph(next, graph, x + DEFAULT_ELEMENT_NODE_WIDTH + DEFAULT_ARROW_WIDTH, y);
        } else {
          paintGraph(next, graph, x + DEFAULT_NODE_WIDTH + DEFAULT_ARROW_WIDTH, y);
        }
      } else {
        paintGraph(next, graph, x + node.getWidth() - next.getWidth(), y);
      }
      graph.addLink(Link.Builder.aLink()
                        .withId(UUIDGenerator.getUuid())
                        .withFrom(node.getId())
                        .withTo(next.getId())
                        .withType(node.getStatus())
                        .build());
    }
  }

  private void extrapolateDimension(Group group) {
    for (Node node : group.getElements()) {
      extrapolateDimension(node);
      if (group.getWidth() < node.getWidth()) {
        group.setWidth(node.getWidth());
      }
      group.setHeight(group.getHeight() + node.getHeight() + DEFAULT_ARROW_HEIGHT);
    }
  }

  private void extrapolateDimension(Node node) {
    node.setWidth(DEFAULT_NODE_WIDTH);
    node.setHeight(DEFAULT_NODE_HEIGHT);

    Group group = node.getGroup();
    if (group != null) {
      extrapolateDimension(group);
      if (node.getWidth() < group.getWidth()) {
        node.setWidth(group.getWidth());
      }
      node.setHeight(node.getHeight() + group.getHeight());
    }

    Node next = node.getNext();
    if (next != null) {
      extrapolateDimension(next);
      if (node.getHeight() < next.getHeight()) {
        node.setHeight(next.getHeight());
      }
      node.setWidth(node.getWidth() + next.getWidth() + DEFAULT_ARROW_WIDTH);
    }
  }
}
