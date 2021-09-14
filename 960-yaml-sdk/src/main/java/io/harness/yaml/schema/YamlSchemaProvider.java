/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema;

import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.yaml.schema.beans.SchemaConstants.CONST_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.DEFINITIONS_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.REF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.REQUIRED_NODE;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.jackson.JsonNodeUtils;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Iterator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlSchemaProvider {
  YamlSchemaHelper yamlSchemaHelper;

  public JsonNode getYamlSchema(EntityType entityType, String orgIdentifier, String projectIdentifier, Scope scope) {
    final YamlSchemaWithDetails schemaDetailsForEntityType = yamlSchemaHelper.getSchemaDetailsForEntityType(entityType);
    final ObjectNode schema = schemaDetailsForEntityType.getSchema().deepCopy();

    try {
      ObjectNode secondLevelNode = getSecondLevelNode(schema);
      ObjectNode secondLevelNodeProperties = getSecondLevelNodeProperties(secondLevelNode);

      if (scope == Scope.ACCOUNT && schemaDetailsForEntityType.isAvailableAtAccountLevel()) {
        JsonNodeUtils.deletePropertiesInJsonNode(secondLevelNodeProperties, PROJECT_KEY, ORG_KEY);
      } else if (scope == Scope.ORG && schemaDetailsForEntityType.isAvailableAtOrgLevel()) {
        JsonNodeUtils.deletePropertiesInJsonNode(secondLevelNodeProperties, PROJECT_KEY);
        if (isNotEmpty(orgIdentifier)) {
          JsonNodeUtils.upsertPropertyInObjectNode(secondLevelNodeProperties.get(ORG_KEY), CONST_NODE, orgIdentifier);
        }
        if (secondLevelNodeProperties.has(ORG_KEY)) {
          JsonNodeUtils.upsertPropertyInObjectNode(secondLevelNode, REQUIRED_NODE, ORG_KEY);
        }
      } else if (scope == Scope.PROJECT && schemaDetailsForEntityType.isAvailableAtProjectLevel()) {
        if (isNotEmpty(orgIdentifier)) {
          JsonNodeUtils.upsertPropertyInObjectNode(secondLevelNodeProperties.get(ORG_KEY), CONST_NODE, orgIdentifier);
        }
        if (isNotEmpty(projectIdentifier)) {
          JsonNodeUtils.upsertPropertyInObjectNode(
              secondLevelNodeProperties.get(PROJECT_KEY), CONST_NODE, projectIdentifier);
        }
        if (secondLevelNodeProperties.has(ORG_KEY) && secondLevelNodeProperties.has(PROJECT_KEY)) {
          JsonNodeUtils.upsertPropertyInObjectNode(secondLevelNode, REQUIRED_NODE, ORG_KEY, PROJECT_KEY);
        }
      }
    } catch (Exception e) {
      log.info("Exception in adding scope to schema. {}", e.getLocalizedMessage());
    }

    // returning original snippet in worst case.
    return schema;
  }

  @VisibleForTesting
  ObjectNode getSecondLevelNodeProperties(JsonNode secondLevelNode) {
    return (ObjectNode) secondLevelNode.get(PROPERTIES_NODE);
  }

  @VisibleForTesting
  ObjectNode getSecondLevelNode(JsonNode schema) {
    ValueNode refNode = null;
    if (schema.get(PROPERTIES_NODE) != null) {
      Iterator<JsonNode> elements = schema.get(PROPERTIES_NODE).elements();
      while (elements.hasNext()) {
        final JsonNode innerNode = elements.next();
        final JsonNode possibleRefNode = innerNode.get(REF_NODE);
        if (possibleRefNode != null) {
          refNode = (ValueNode) possibleRefNode;
          break;
        }
      }
    }
    if (refNode == null) {
      // No refNode at first level hence returning original schema.
      throw new io.harness.yaml.schema.YamlSchemaException("No ref node found at first level");
    }

    String refNodeValue = refNode.textValue().split("/")[2];
    return (ObjectNode) schema.get(DEFINITIONS_NODE).get(refNodeValue);
  }

  public JsonNode updateArrayFieldAtSecondLevelInSchema(JsonNode schema, String nodeKey, String key, String... values) {
    final JsonNode yamlSchema = schema.deepCopy();
    try {
      final ObjectNode objectNodeRemovingWrapper = getSecondLevelNodeProperties(getSecondLevelNode(yamlSchema));
      JsonNodeUtils.setPropertiesInJsonNodeWithArrayKey(
          (ObjectNode) objectNodeRemovingWrapper.get(nodeKey), key, values);
    } catch (Exception e) {
      log.error("Encountered error while setting  for key: {}", nodeKey, e);
    }
    return yamlSchema;
  }
  public JsonNode upsertInObjectFieldAtSecondLevelInSchema(JsonNode schema, String nodeKey, String key, String value) {
    JsonNode yamlSchema = schema.deepCopy();
    try {
      final ObjectNode objectNodeRemovingWrapper = getSecondLevelNodeProperties(getSecondLevelNode(yamlSchema));
      JsonNodeUtils.upsertPropertyInObjectNode((ObjectNode) objectNodeRemovingWrapper.get(nodeKey), key, value);
    } catch (Exception e) {
      log.error("Encountered error while setting  for key: {}", nodeKey, e);
    }
    return yamlSchema;
  }
}
