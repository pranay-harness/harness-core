/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exceptions.DuplicateAliasException;
import io.harness.utils.RecastReflectionUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.PIPELINE)
public class AliasRegistry {
  private static AliasRegistry SINGLETON;
  private static final Map<String, Class<?>> aliasesMap = new ConcurrentHashMap<>();

  private static final Set<String> packages = new HashSet<>();

  public static AliasRegistry getInstance() {
    if (SINGLETON == null) {
      SINGLETON = new AliasRegistry();
    }

    return SINGLETON;
  }

  private AliasRegistry() {}

  public void register(Class<?> clazz) {
    String recastAliasValue = RecastReflectionUtils.obtainRecasterAliasValueOrNull(clazz);
    if (recastAliasValue == null) {
      return;
    }

    if (aliasesMap.containsKey(recastAliasValue)) {
      throw new DuplicateAliasException(String.format("%s alias for %s class was already used by %s class",
          recastAliasValue, clazz.getName(), aliasesMap.get(recastAliasValue).getName()));
    }

    aliasesMap.put(recastAliasValue, clazz);
  }

  public Class<?> obtain(String alias) {
    return aliasesMap.get(alias);
  }

  public void addPackages(String... packageNames) {
    Collections.addAll(packages, packageNames);
  }

  public boolean shouldContainAlias(Class<?> clazz) {
    final String clazzPkg = clazz.getCanonicalName();
    if (clazzPkg == null) {
      return false;
    }

    for (String pkg : packages) {
      if (clazzPkg.startsWith(pkg)) {
        return true;
      }
    }

    return false;
  }
}
