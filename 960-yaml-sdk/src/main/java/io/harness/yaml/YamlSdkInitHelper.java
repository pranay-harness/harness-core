/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.YamlSchemaGenerator;
import io.harness.yaml.schema.YamlSchemaHelper;
import io.harness.yaml.snippets.helper.YamlSnippetHelper;
import io.harness.yaml.validator.YamlSchemaValidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Injector;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class YamlSdkInitHelper {
  public static void initialize(Injector injector, YamlSdkConfiguration yamlSdkConfiguration) {
    YamlSchemaGenerator yamlSchemaGenerator = injector.getInstance(YamlSchemaGenerator.class);
    Map<EntityType, JsonNode> schemas = yamlSchemaGenerator.generateYamlSchema();
    YamlSnippetHelper yamlSnippetHelper = injector.getInstance(YamlSnippetHelper.class);
    YamlSchemaValidator yamlSchemaValidator = injector.getInstance(YamlSchemaValidator.class);
    YamlSchemaHelper yamlSchemaHelper = injector.getInstance(YamlSchemaHelper.class);
    if (isEmpty(schemas)) {
      return;
    }
    if (yamlSdkConfiguration.isRequireSnippetInit()) {
      yamlSnippetHelper.initializeSnippets();
    }
    if (yamlSdkConfiguration.isRequireValidatorInit()) {
      yamlSchemaValidator.initializeValidatorWithSchema(schemas);
    }
    if (yamlSdkConfiguration.isRequireSchemaInit()) {
      yamlSchemaHelper.initializeSchemaMaps(schemas);
    }
  }
}
