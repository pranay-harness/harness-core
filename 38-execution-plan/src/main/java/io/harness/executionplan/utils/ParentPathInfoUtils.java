package io.harness.executionplan.utils;

import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.plancreator.beans.PlanLevelNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParentPathInfoUtils {
  public final String PARENT_PATH_INFO = "PARENT_PATH_INFO";

  private <T> void setConfig(String key, T config, ExecutionPlanCreationContext context) {
    if (config == null) {
      context.removeAttribute(key);
    } else {
      context.addAttribute(key, config);
    }
  }

  private <T> Optional<T> getConfig(String key, ExecutionPlanCreationContext context) {
    return context.getAttribute(key);
  }

  public void addToParentPath(ExecutionPlanCreationContext context, PlanLevelNode planLevelNode) {
    Optional<LinkedList<PlanLevelNode>> parentPath = getConfig(PARENT_PATH_INFO, context);
    LinkedList<PlanLevelNode> planLevelNodes = parentPath.orElse(new LinkedList<>());
    planLevelNodes.addLast(planLevelNode);
    setConfig(PARENT_PATH_INFO, planLevelNodes, context);
  }

  /**
   * Removes the last levelNode from path.
   * @param context
   */
  public void removeFromParentPath(ExecutionPlanCreationContext context) {
    Optional<LinkedList<PlanLevelNode>> parentPath = getConfig(PARENT_PATH_INFO, context);
    LinkedList<PlanLevelNode> planLevelNodes =
        parentPath.orElseThrow(() -> new InvalidArgumentsException("Parent Path has not been initialised."));
    planLevelNodes.removeLast();
    setConfig(PARENT_PATH_INFO, planLevelNodes, context);
  }

  /**
   * Gets the current level parent path.
   * @param context
   * @return Parent path string.
   */
  public String getParentPath(ExecutionPlanCreationContext context) {
    Optional<LinkedList<PlanLevelNode>> parentPathOptional = getConfig(PARENT_PATH_INFO, context);
    LinkedList<PlanLevelNode> planLevelNodes = parentPathOptional.orElse(new LinkedList<>());
    return planLevelNodes.stream().map(PlanLevelNode::getIdentifier).collect(Collectors.joining("."));
  }

  /**
   * Gets parent path upto last node as given planNodeType.
   * @param planNodeType
   * @param context
   * @return Parent path string upto last occurrence of given planNodeType.
   */
  public String getParentPath(String planNodeType, ExecutionPlanCreationContext context) {
    Optional<List<PlanLevelNode>> parentPathOptional = getConfig(PARENT_PATH_INFO, context);
    if (parentPathOptional.isPresent()) {
      List<PlanLevelNode> planLevelNodes = parentPathOptional.get();
      String parentPath = "";
      boolean foundNode = false;
      for (int i = planLevelNodes.size() - 1; i >= 0; i--) {
        if (planLevelNodes.get(i).getPlanNodeType().equals(planNodeType)) {
          foundNode = true;
          parentPath = IntStream.rangeClosed(0, i)
                           .mapToObj(j -> planLevelNodes.get(j).getIdentifier())
                           .collect(Collectors.joining("."));
        }
      }
      if (!foundNode) {
        throw new InvalidArgumentsException("PlanNode type doesn't exist in parent path.");
      }
      return parentPath;
    } else {
      throw new InvalidArgumentsException("Parent Path has not been initialised.");
    }
  }
}
