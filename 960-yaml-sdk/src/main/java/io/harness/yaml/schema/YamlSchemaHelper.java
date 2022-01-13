/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class YamlSchemaHelper {
  static Map<EntityType, YamlSchemaWithDetails> entityTypeSchemaMap = new HashMap<>();
  List<YamlSchemaRootClass> yamlSchemaRootClasses;

  @Inject
  public YamlSchemaHelper(List<YamlSchemaRootClass> yamlSchemaRootClasses) {
    this.yamlSchemaRootClasses = yamlSchemaRootClasses;
  }

  public void initializeSchemaMaps(Map<EntityType, JsonNode> schemas) {
    if (isNotEmpty(yamlSchemaRootClasses)) {
      yamlSchemaRootClasses.forEach(yamlSchemaRootClass -> {
        final EntityType entityType = yamlSchemaRootClass.getEntityType();
        try {
          JsonNode schemaJson = schemas.get(yamlSchemaRootClass.getEntityType());
          final YamlSchemaWithDetails yamlSchemaWithDetails =
              YamlSchemaWithDetails.builder()
                  .isAvailableAtAccountLevel(yamlSchemaRootClass.isAvailableAtAccountLevel())
                  .isAvailableAtOrgLevel(yamlSchemaRootClass.isAvailableAtOrgLevel())
                  .schemaClassName(yamlSchemaRootClass.getClazz().getSimpleName())
                  .yamlSchemaMetadata(yamlSchemaRootClass.getYamlSchemaMetadata())
                  .isAvailableAtProjectLevel(yamlSchemaRootClass.isAvailableAtProjectLevel())
                  .schema(schemaJson)
                  .build();
          entityTypeSchemaMap.put(entityType, yamlSchemaWithDetails);
        } catch (Exception e) {
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
