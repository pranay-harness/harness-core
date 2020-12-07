package io.harness.yaml.validator;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.constants.YamlConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

public class YamlSchemaValidator {
  @Inject YamlSdkConfiguration yamlSdkConfiguration;

  /**
   * @param yaml       The yaml String which is to be validated against schema of entity.
   * @param entityType The entityType against which yaml string needs to be validated.
   * @return Set of error messages. Will be empty if we don't encounter any error.
   * @throws IOException when yaml string could't be parsed.
   */
  public Set<String> validate(String yaml, EntityType entityType) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                                    .objectMapper(mapper)
                                    .build();
    JsonSchema schema = factory.getSchema(getSchemaFromResource(entityType));
    JsonNode jsonNode = mapper.readTree(yaml);
    Set<ValidationMessage> validateMsg = schema.validate(jsonNode);
    return validateMsg.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
  }

  @VisibleForTesting
  InputStream getSchemaFromResource(EntityType entityType) {
    return YamlSchemaValidator.class.getClassLoader().getResourceAsStream(getSchemaPathFromEntityType(entityType));
  }

  private String getSchemaPathFromEntityType(EntityType entityType) {
    final String yamlName = entityType.getYamlName();
    final String schemaBasePath = yamlSdkConfiguration.getSchemaBasePath();
    String resourcePath = yamlName + File.separator + YamlConstants.SCHEMA_FILE_NAME;
    return isEmpty(schemaBasePath) ? resourcePath : schemaBasePath + File.separator + yamlName + resourcePath;
  }
}
