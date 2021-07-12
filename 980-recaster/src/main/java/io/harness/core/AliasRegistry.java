package io.harness.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exceptions.DuplicateAliasException;
import io.harness.utils.RecastReflectionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.PIPELINE)
public class AliasRegistry {
  private static AliasRegistry SINGLETON;
  private static final Map<String, Class<?>> aliasesMap = new ConcurrentHashMap<>();

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
}
