/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

@Slf4j
public class ConnectorTypeInformationMapper implements TypeInformationMapper {
  private Map<Class<?>, Alias> typeToAliasMap;
  private Map<Alias, Class<?>> aliasToTypeMap;

  @Builder
  public ConnectorTypeInformationMapper(Map<String, Class<?>> aliasMap) {
    this.typeToAliasMap = new ConcurrentHashMap<>();
    this.aliasToTypeMap = new ConcurrentHashMap<>();
    populateTypeMap(aliasMap);
  }

  private void populateTypeMap(Map<String, Class<?>> aliasMap) {
    aliasMap.forEach((k, v) -> {
      typeToAliasMap.put(v, Alias.of(k));
      aliasToTypeMap.put(Alias.of(k), v);
    });
  }

  @Nullable
  @Override
  public TypeInformation<?> resolveTypeFrom(Alias alias) {
    String stringAlias = alias.mapTyped(String.class);
    if (stringAlias != null) {
      Class<?> clazz = aliasToTypeMap.get(alias);
      if (clazz != null) {
        return ClassTypeInformation.from(clazz);
      }
      log.error("No Class recorded for Alias: {}", stringAlias);
      return loadClass(stringAlias).orElse(null);
    }
    return null;
  }

  @Override
  public Alias createAliasFor(TypeInformation<?> type) {
    ClassTypeInformation<?> typeClass = (ClassTypeInformation<?>) type;
    if (typeToAliasMap.containsKey(typeClass.getType())) {
      return typeToAliasMap.get(typeClass.getType());
    }
    log.error("No Alias recorded for Connector Entity {}. Using fully qualified path", type.getType().getName());
    return Alias.of(type.getType().getName());
  }

  private static Optional<ClassTypeInformation<?>> loadClass(String typeName) {
    try {
      return Optional.of(ClassTypeInformation.from(ClassUtils.forName(typeName, null)));
    } catch (ClassNotFoundException e) {
      return Optional.empty();
    }
  }
}
