package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.StandardCharset;
import io.swagger.models.Model;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public interface AbstractSchemaChecker {
  ClassLoader getClassLoader();

  String getSchemaBasePath();

  default void ensureSchemaUpdated() throws IOException {
    FilterBuilder filter = new FilterBuilder().include(FilterBuilder.prefix("io.harness")).include("software.wings");
    Reflections reflections = new Reflections(ConfigurationBuilder.build()
                                                  .forPackages("io.harness", "software.wings")
                                                  .filterInputsBy(filter)
                                                  .addClassLoaders(getClassLoader()));
    final Set<Class<?>> schemaRoots = reflections.getTypesAnnotatedWith(YamlSchemaRoot.class);

    if (isEmpty(schemaRoots)) {
      return;
    }
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
    YamlSchemaGenerator yamlSchemaGenerator = new YamlSchemaGenerator();
    ObjectMapper objectMapper = new ObjectMapper();
    for (Class<?> schemaRoot : schemaRoots) {
      final EntityType entityType = schemaRoot.getDeclaredAnnotation(YamlSchemaRoot.class).value();
      final String schemaBasePath = getSchemaBasePath();
      final String schemaPathForEntityType = YamlSchemaUtils.getSchemaPathForEntityType(entityType, schemaBasePath);
      final Map<String, Model> stringModelMap = swaggerGenerator.generateDefinitions(schemaRoot);
      final Map<String, Set<FieldSubtypeData>> subTypeMap = yamlSchemaGenerator.getSubTypeMap(schemaRoot);
      final Map<String, JsonNode> stringJsonNodeMap = yamlSchemaGenerator.generateDefinitions(
          stringModelMap, "", subTypeMap, YamlSchemaUtils.getSwaggerName(schemaRoot));
      final JsonNode jsonNode = stringJsonNodeMap.get("/all.json");
      final String resource =
          IOUtils.resourceToString(schemaPathForEntityType, StandardCharset.UTF_8, getClassLoader());
      final JsonNode jsonNode1 = objectMapper.readTree(resource);
      if (!jsonNode.equals(jsonNode1)) {
        throw new InvalidRequestException("Schema doesnt match");
      }
    }
  }
}
