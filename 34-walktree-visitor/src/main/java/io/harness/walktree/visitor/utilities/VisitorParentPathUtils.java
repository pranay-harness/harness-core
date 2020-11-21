package io.harness.walktree.visitor.utilities;

import io.harness.exception.InvalidArgumentsException;
import io.harness.walktree.beans.LevelNode;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VisitorParentPathUtils {
  private final String PARENT_PATH_KEY = "PARENT_PATH_KEY";
  public final String PATH_CONNECTOR = ".";

  private <T> void setConfig(String key, T config, Map<String, Object> contextMap) {
    if (config == null) {
      contextMap.remove(key);
    } else {
      contextMap.put(key, config);
    }
  }

  private <T> Optional<T> getConfig(String key, Map<String, Object> contextMap) {
    return Optional.ofNullable((T) contextMap.get(key));
  }

  public void addToParentList(Map<String, Object> contextMap, LevelNode levelNode) {
    Optional<LinkedList<LevelNode>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<LevelNode> levelNodes = parentPath.orElse(new LinkedList<>());
    levelNodes.addLast(levelNode);
    setConfig(PARENT_PATH_KEY, levelNodes, contextMap);
  }

  public void removeFromParentList(Map<String, Object> contextMap) {
    Optional<LinkedList<LevelNode>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<LevelNode> levelNodes =
        parentPath.orElseThrow(() -> new InvalidArgumentsException("Parent Path has not been initialised."));
    levelNodes.removeLast();
  }

  public String getFullQualifiedDomainName(Map<String, Object> contextMap) {
    Optional<LinkedList<LevelNode>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<LevelNode> levelNodes = parentPath.orElse(new LinkedList<>());
    return levelNodes.stream().map(LevelNode::getQualifierName).collect(Collectors.joining(PATH_CONNECTOR));
  }
}
