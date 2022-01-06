/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
