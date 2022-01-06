/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.validator;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlSchemaValidator {
  public static Map<EntityType, JsonSchema> schemas = new HashMap<>();
  ObjectMapper mapper;
  List<YamlSchemaRootClass> yamlSchemaRootClasses;

  @Inject
  public YamlSchemaValidator(List<YamlSchemaRootClass> yamlSchemaRootClasses) {
    mapper = new ObjectMapper(new YAMLFactory());
    this.yamlSchemaRootClasses = yamlSchemaRootClasses;
  }

  /**
   * @param yaml       The yaml String which is to be validated against schema of entity.
   * @param entityType The entityType against which yaml string needs to be validated.
   * @return Set of error messages. Will be empty if we don't encounter any error.
   * @throws IOException when yaml string could't be parsed.
   */
  public Set<String> validate(String yaml, EntityType entityType) throws IOException {
    if (!schemas.containsKey(entityType)) {
      throw new InvalidRequestException("No schema found for entityType.");
    }
    JsonSchema schema = schemas.get(entityType);
    return validate(yaml, schema);
  }

  public Set<String> validate(String yaml, JsonSchema schema) throws IOException {
    JsonNode jsonNode = mapper.readTree(yaml);
    Set<ValidationMessage> validateMsg = schema.validate(jsonNode);
    return validateMsg.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
  }

  public Set<String> validate(String yaml, String stringSchema) throws IOException {
    JsonNode jsonNode = mapper.readTree(yaml);
    JsonSchemaFactory factory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
    JsonSchema schema = factory.getSchema(stringSchema);
    Set<ValidationMessage> validateMsg = schema.validate(jsonNode);
    return validateMsg.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
  }

  public void populateSchemaInStaticMap(JsonNode schema, EntityType entityType) {
    JsonSchemaFactory factory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
    try {
      final JsonSchema jsonSchema = factory.getSchema(schema);
      schemas.put(entityType, jsonSchema);
    } catch (Exception e) {
      throw new InvalidRequestException(String.format("Couldn't parse schema for entity: %s", entityType), e);
    }
  }

  /**
   * Initialises a static map which will help in fast validation against a schema.
   *
   */
  public void initializeValidatorWithSchema(Map<EntityType, JsonNode> schemas) {
    schemas.forEach((entityType, jsonNode) -> populateSchemaInStaticMap(jsonNode, entityType));
  }
}
