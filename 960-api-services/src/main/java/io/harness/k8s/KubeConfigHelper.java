/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.kubernetes.client.util.KubeConfig;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class KubeConfigHelper {
  public static final String NAME = "name";
  public static final String CONTEXT = "context";
  public static final String USER = "user";

  private KubeConfigHelper() {}

  @Nullable
  public static Map<String, Object> findObject(List<Object> list, String name) {
    if (isEmpty(list)) {
      return null;
    }

    Iterator<Object> iter = list.iterator();
    Map<String, Object> result;
    do {
      if (!iter.hasNext()) {
        return null;
      }

      result = (Map<String, Object>) iter.next();
    } while (!name.equals(result.get(NAME)));

    return result;
  }

  @Nullable
  public static String getCurrentUser(KubeConfig kubeConfig) {
    Map<String, Object> context = findObject(kubeConfig.getContexts(), kubeConfig.getCurrentContext());
    if (context == null) {
      return null;
    }

    Map<String, Object> currentContext = (Map<String, Object>) context.get(CONTEXT);
    if (currentContext == null) {
      return null;
    }

    return currentContext.containsKey(USER) ? (String) currentContext.get(USER) : null;
  }
}
