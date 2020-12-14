package io.harness.yaml.validator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Singleton
@Slf4j
public class YamlSchemaValidator {
  public static Map<EntityType, JsonSchema> schemas = new HashMap<>();
  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

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

  /**
   * Finds all classes with {@link YamlSchemaRoot} in classpath and store its schema in schemas map.
   */
  public void populateSchemaInStaticMap(String schemaBasePath) {
    final Set<Class<?>> rootClasses = YamlSchemaUtils.getClasses(null, YamlSchemaUtils.class);
    if (isNotEmpty(rootClasses)) {
      JsonSchemaFactory factory =
          JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
      rootClasses.forEach(rootClass -> {
        final EntityType entityType = rootClass.getAnnotation(YamlSchemaRoot.class).value();
        final String schemaPathFromEntityType = YamlSchemaUtils.getSchemaPathForEntityType(entityType, schemaBasePath);
        try {
          final String schema = IOUtils.resourceToString(
              schemaPathFromEntityType, StandardCharsets.UTF_8, YamlSchemaValidator.class.getClassLoader());
          if (isNotEmpty(schema)) {
            final JsonSchema jsonSchema = factory.getSchema(schema);
            schemas.put(entityType, jsonSchema);
          }
        } catch (Exception e) {
          throw new InvalidRequestException(String.format("Couldn't load schema for entity: %s", entityType), e);
        }
      });
    }
  }
}
