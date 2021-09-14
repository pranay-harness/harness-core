/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityReferenceHelper {
  public static String createFQN(List<String> hierarchyList) {
    StringBuilder fqnString = new StringBuilder(32);
    hierarchyList.forEach(s -> {
      if (EmptyPredicate.isEmpty(s)) {
        throw new InvalidArgumentsException("Hierarchy identifier cannot be empty/null");
      }
      fqnString.append(s).append('/');
    });
    return fqnString.toString();
  }
}
