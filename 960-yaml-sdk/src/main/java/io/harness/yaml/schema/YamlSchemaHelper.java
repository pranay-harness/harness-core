package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  static Map<EntityType, YamlSchemaWithDetails> entityTypeSchemaMap = new HashMap<>();

  public void initializeSchemaMaps(String schemaBasePath, Set<Class<?>> classes) {
    ObjectMapper objectMapper = new ObjectMapper();
    if (isNotEmpty(classes)) {
      classes.forEach(clazz -> {
        final YamlSchemaRoot annotation = clazz.getAnnotation(YamlSchemaRoot.class);
        final EntityType entityType = annotation.value();
        final String schemaPathForEntityType = YamlSchemaUtils.getSchemaPathForEntityType(entityType, schemaBasePath);
        try {
          final String schema =
              IOUtils.resourceToString(schemaPathForEntityType, StandardCharsets.UTF_8, clazz.getClassLoader());
          if (isNotEmpty(schema)) {
            JsonNode schemaJson = objectMapper.readTree(schema);
            final YamlSchemaWithDetails yamlSchemaWithDetails =
                YamlSchemaWithDetails.builder()
                    .isAvailableAtAccountLevel(annotation.availableAtAccountLevel())
                    .isAvailableAtOrgLevel(annotation.availableAtOrgLevel())
                    .isAvailableAtProjectLevel(annotation.availableAtProjectLevel())
                    .schema(schemaJson)
                    .build();
            entityTypeSchemaMap.put(entityType, yamlSchemaWithDetails);
          }
        } catch (IOException e) {
          throw new InvalidRequestException(
              String.format("Cannot initialize Yaml Schema for entity type: %s", entityType), e);
        }
      });
    }
  }

  public YamlSchemaWithDetails getSchemaDetailsForEntityType(EntityType entityType) {
    if (!entityTypeSchemaMap.containsKey(entityType)) {
      throw new InvalidRequestException(String.format("No Schema for entity type: %s", entityType));
    }
    return entityTypeSchemaMap.get(entityType);
  }
}
