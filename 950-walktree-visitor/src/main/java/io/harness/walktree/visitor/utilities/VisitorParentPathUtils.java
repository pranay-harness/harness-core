/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.utilities;

import io.harness.exception.InvalidArgumentsException;

import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VisitorParentPathUtils {
  public final String PARENT_PATH_KEY = "PARENT_PATH_KEY";
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

  public void addToParentList(Map<String, Object> contextMap, String qualifiedName) {
    Optional<LinkedList<String>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<String> qualifiedNameLists = parentPath.orElse(new LinkedList<>());
    qualifiedNameLists.addLast(qualifiedName);
    setConfig(PARENT_PATH_KEY, qualifiedNameLists, contextMap);
  }

  public void removeFromParentList(Map<String, Object> contextMap) {
    Optional<LinkedList<String>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<String> qualifiedNameLists =
        parentPath.orElseThrow(() -> new InvalidArgumentsException("Parent Path has not been initialised."));
    qualifiedNameLists.removeLast();
  }

  public String getFullQualifiedDomainName(Map<String, Object> contextMap) {
    Optional<LinkedList<String>> parentPath = getConfig(PARENT_PATH_KEY, contextMap);
    LinkedList<String> qualifiedNameLists = parentPath.orElse(new LinkedList<>());
    return qualifiedNameLists.stream().filter(Objects::nonNull).collect(Collectors.joining(PATH_CONNECTOR));
  }
}
