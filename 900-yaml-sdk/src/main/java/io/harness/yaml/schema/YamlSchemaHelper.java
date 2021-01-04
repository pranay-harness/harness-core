package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSchemaRoot;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.IOUtils;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YamlSchemaHelper {
  static Map<EntityType, String> entityTypeSchemaMap = new HashMap<>();

  public void initializeSchemaMaps(String schemaBasePath, Set<Class<?>> classes) {
    if (isNotEmpty(classes)) {
      classes.forEach(clazz -> {
        final EntityType entityType = clazz.getAnnotation(YamlSchemaRoot.class).value();
        final String schemaPathForEntityType = YamlSchemaUtils.getSchemaPathForEntityType(entityType, schemaBasePath);
        try {
          final String schema =
              IOUtils.resourceToString(schemaPathForEntityType, StandardCharsets.UTF_8, clazz.getClassLoader());
          if (isNotEmpty(schema)) {
            entityTypeSchemaMap.put(entityType, schema);
          }
        } catch (IOException e) {
          throw new InvalidRequestException(
              String.format("Cannot initialize Yaml Schema for entity type: %s", entityType), e);
        }
      });
    }
  }

  public String getSchemaForEntityType(EntityType entityType) {
    if (!entityTypeSchemaMap.containsKey(entityType)) {
      throw new InvalidRequestException(String.format("No Schema for entity type: %s", entityType));
    }
    return entityTypeSchemaMap.get(entityType);
  }
}
