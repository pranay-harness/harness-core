/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import io.harness.ConnectorConstants;
import io.harness.EntitySubtype;
import io.harness.EntityType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.SchemaConstants;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class YamlSchemaSubtypeHelper {
  /**
   * Keeping once calculated result in the map so that if query comes again we can serve from map.
   */
  private static Map<Pair<EntityType, EntitySubtype>, String> subtypeSchemaMap = new HashMap<>();
  private ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Currently only handled for connectors, later if required could be extended by converting to
   * handlers based on individual subtype or if its generic we can move to sdk.
   * <p>
   * TODO: Optimize to also clear other definitions which aren't required.
   */
  public String getSchemaForSubtype(EntityType entityType, EntitySubtype entitySubtype, String schema) {
    if (entityType != EntityType.CONNECTORS || !(entitySubtype instanceof ConnectorType)) {
      throw new InvalidRequestException("Subtypes only supported for Connectors.");
    }
    if (subtypeSchemaMap.containsKey(Pair.of(entityType, entitySubtype))) {
      return subtypeSchemaMap.get(Pair.of(entityType, entitySubtype));
    }
    try {
      ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(schema);
      ArrayNode enumArrayNode = getSubtypeNode(jsonNode);
      final String connectorType = ((ConnectorType) entitySubtype).getDisplayName();
      enumArrayNode.removeAll();
      enumArrayNode.add(connectorType);
      final String subtypeSchema = objectMapper.writeValueAsString(jsonNode);
      subtypeSchemaMap.put(Pair.of(entityType, entitySubtype), subtypeSchema);
      return subtypeSchema;
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot convert schema to json.", e);
    }
  }

  @VisibleForTesting
  ArrayNode getSubtypeNode(ObjectNode jsonNode) {
    ObjectNode definitionsNode = (ObjectNode) jsonNode.get(SchemaConstants.DEFINITIONS_NODE);
    final String swaggerName = YamlSchemaUtils.getSwaggerName(ConnectorInfoDTO.class);
    ObjectNode connectorInfoNode = (ObjectNode) definitionsNode.get(swaggerName);
    ObjectNode propertiesNode = (ObjectNode) connectorInfoNode.get(SchemaConstants.PROPERTIES_NODE);
    ObjectNode typeNode = (ObjectNode) propertiesNode.get(ConnectorConstants.CONNECTOR_TYPES);
    return (ArrayNode) typeNode.get(SchemaConstants.ENUM_NODE);
  }
}
