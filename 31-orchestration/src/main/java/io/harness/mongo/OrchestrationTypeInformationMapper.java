package io.harness.mongo;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

@Slf4j
public class OrchestrationTypeInformationMapper implements TypeInformationMapper {
  private Map<Class<?>, Alias> typeToAliasMap;
  private Map<Alias, Class<?>> aliasToTypeMap;

  @Builder
  public OrchestrationTypeInformationMapper(Map<String, Class<?>> aliasMap) {
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
      return loadClass(stringAlias)
          .orElseThrow(() -> new OrchestrationAliasRegistryException("No class registered for alias : ", stringAlias));
    }
    return null;
  }

  @Override
  public Alias createAliasFor(TypeInformation<?> type) {
    ClassTypeInformation<?> typeClass = (ClassTypeInformation<?>) type;
    if (typeToAliasMap.containsKey(typeClass.getType())) {
      return typeToAliasMap.get(typeClass.getType());
    }
    throw new OrchestrationAliasRegistryException("No Alias Found for class : ", typeClass.getType().getName());
  }

  private static Optional<ClassTypeInformation<?>> loadClass(String typeName) {
    try {
      return Optional.of(ClassTypeInformation.from(ClassUtils.forName(typeName, null)));
    } catch (ClassNotFoundException e) {
      return Optional.empty();
    }
  }
}