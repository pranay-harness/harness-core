/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.yaml.schema.beans.SchemaConstants.CONST_NODE;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.ngtriggers.service.NGTriggerYamlSchemaService;
import io.harness.yaml.schema.YamlSchemaProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerYamlSchemaServiceImpl implements NGTriggerYamlSchemaService {
  private final YamlSchemaProvider yamlSchemaProvider;

  @Override
  public JsonNode getTriggerYamlSchema(String projectIdentifier, String orgIdentifier, String identifier, Scope scope) {
    JsonNode schema = yamlSchemaProvider.getYamlSchema(EntityType.TRIGGERS, orgIdentifier, projectIdentifier, scope);

    if (schema != null && isNotEmpty(identifier)) {
      schema = yamlSchemaProvider.upsertInObjectFieldAtSecondLevelInSchema(
          schema, NGCommonEntityConstants.IDENTIFIER_KEY, CONST_NODE, identifier);
    }

    return schema;
  }
}
