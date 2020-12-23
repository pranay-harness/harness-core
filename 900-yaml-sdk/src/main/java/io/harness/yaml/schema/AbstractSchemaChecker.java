package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.exception.InvalidRequestException;
import io.harness.packages.HarnessPackages;
import io.harness.validation.OneOfField;
import io.harness.validation.OneOfFields;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;
import io.harness.yamlSchema.YamlSchemaRoot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.StandardCharset;
import io.swagger.models.Model;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public interface AbstractSchemaChecker {
  ClassLoader getClassLoader();

  String getSchemaBasePath();

  default void ensureSchemaUpdated() throws IOException {
    Reflections reflections = getReflections();
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
      final Map<String, SwaggerDefinitionsMetaInfo> subTypeMap = yamlSchemaGenerator.getSwaggerMetaInfo(schemaRoot);
      final Map<String, JsonNode> stringJsonNodeMap = yamlSchemaGenerator.generateDefinitions(
          stringModelMap, "", subTypeMap, YamlSchemaUtils.getSwaggerName(schemaRoot));
      final JsonNode jsonNode = stringJsonNodeMap.get("/all.json");
      final String resource =
          IOUtils.resourceToString(schemaPathForEntityType, StandardCharset.UTF_8, getClassLoader());
      final JsonNode jsonNode1 = objectMapper.readTree(resource);
      if (!jsonNode.equals(jsonNode1)) {
        throw new InvalidRequestException(String.format("Schema doesnt match for %s", entityType));
      }
    }
  }

  default Reflections getReflections() {
    FilterBuilder filter = new FilterBuilder()
                               .include(FilterBuilder.prefix(HarnessPackages.IO_HARNESS))
                               .include(HarnessPackages.SOFTWARE_WINGS);
    return new Reflections(ConfigurationBuilder.build()
                               .forPackages(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS)
                               .filterInputsBy(filter)
                               .addClassLoaders(getClassLoader()));
  }

  default void ensureOneOfHasCorrectValues() throws NoSuchFieldException {
    final Set<Class<?>> typesAnnotatedWith = getReflections().getTypesAnnotatedWith(OneOfField.class);
    for (Class<?> clazz : typesAnnotatedWith) {
      final OneOfField annotation = clazz.getAnnotation(OneOfField.class);
      validateForOneOfFields(clazz, annotation);
    }
    final Set<Class<?>> typesAnnotated = getReflections().getTypesAnnotatedWith(OneOfFields.class);
    for (Class<?> clazz : typesAnnotated) {
      final OneOfFields annotation = clazz.getAnnotation(OneOfFields.class);
      final OneOfField[] value = annotation.value();
      for (OneOfField oneOfField : value) {
        validateForOneOfFields(clazz, oneOfField);
      }
    }
  }

  default void validateForOneOfFields(Class<?> clazz, OneOfField annotation) {
    final String[] fields = annotation.fields();
    final Field[] declaredFieldsInClass = clazz.getDeclaredFields();
    final Set<String> decFieldSwaggerName =
        Arrays.stream(declaredFieldsInClass).map(YamlSchemaUtils::getFieldName).collect(Collectors.toSet());
    for (String field : fields) {
      if (!decFieldSwaggerName.contains(field)) {
        throw new InvalidRequestException(String.format("Field %s has incorrect Name", field));
      }
    }
  }
}
