/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class LogKeyUtils {
  private static Map<Class, String> entityIdName = new ConcurrentHashMap<>();

  public static String logKeyForId(Class entityClass) {
    return entityIdName.computeIfAbsent(entityClass, cls -> {
      final String key = calculateLogKeyForId(cls);
      log.warn("Provide logKeyForId for the class {} instead of using the default one", cls.getName());
      return key;
    });
  }

  @SuppressWarnings("squid:S1872")
  public static String calculateLogKeyForId(Class cls) {
    if (!(UuidAccess.class.isAssignableFrom(cls))) {
      return "";
    }

    while (UuidAccess.class.isAssignableFrom(cls.getSuperclass())) {
      // TODO: remove when base is removed
      if ("Base".equals(cls.getSuperclass().getSimpleName())) {
        break;
      }
      cls = cls.getSuperclass();
    }

    String name = cls.getSimpleName() + "Id";

    char[] c = name.toCharArray();
    c[0] = Character.toLowerCase(c[0]);
    return new String(c);
  }
}
