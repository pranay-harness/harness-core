package io.harness.yaml.schema;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.packages.HarnessPackages.IO_HARNESS;
import static io.harness.packages.HarnessPackages.SOFTWARE_WINGS;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.validation.OneOfField;
import io.harness.validation.OneOfFields;
import io.harness.yaml.YamlSdkInitConstants;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

@Slf4j
@Singleton
@OwnedBy(DX)
public class AbstractSchemaChecker {
  public void schemaTests(List<YamlSchemaRootClass> yamlSchemaRootClasses, ObjectMapper objectMapper)
      throws IOException, ClassNotFoundException {
    Reflections reflections = new Reflections(IO_HARNESS, SOFTWARE_WINGS);
    ensureSchemaUpdated(yamlSchemaRootClasses, objectMapper);
    ensureOneOfHasCorrectValues(reflections);
  }

  void ensureSchemaUpdated(List<YamlSchemaRootClass> yamlSchemaRootClasses, ObjectMapper objectMapper)
      throws IOException, ClassNotFoundException {
    if (isEmpty(yamlSchemaRootClasses)) {
      return;
    }
    YamlSchemaGenerator yamlSchemaGenerator =
        new YamlSchemaGenerator(new JacksonClassHelper(), new SwaggerGenerator(objectMapper), yamlSchemaRootClasses);
    final Map<EntityType, JsonNode> entityTypeJsonNodeMap = yamlSchemaGenerator.generateYamlSchema();
    final ObjectWriter objectWriter = yamlSchemaGenerator.getObjectWriter();
    for (YamlSchemaRootClass schemaRoot : yamlSchemaRootClasses) {
      log.info("Running schema check for {}", schemaRoot.getEntityType());
      final String schemaBasePath = YamlSdkInitConstants.schemaBasePath;
      final String schemaPathForEntityType =
          YamlSchemaUtils.getSchemaPathForEntityType(schemaRoot.getEntityType(), schemaBasePath);
      final JsonNode jsonNode = entityTypeJsonNodeMap.get(schemaRoot.getEntityType());
      final String s = objectWriter.writeValueAsString(jsonNode);
      // todo(abhinav): find a better way to find caller class.
      String callerName = Thread.currentThread().getStackTrace()[10].getClassName();
      final Class<?> clazz = Class.forName(callerName);
      try {
        final String schemaInUT =
            IOUtils.resourceToString(schemaPathForEntityType, StandardCharsets.UTF_8, clazz.getClassLoader());
        if (!schemaInUT.equals(s)) {
          log.info("Difference in schema :\n" + StringUtils.difference(s, schemaInUT));
          throw new YamlSchemaException(String.format("Yaml schema not updated for %s", schemaRoot.getEntityType()));
        }
        log.info("schema check success for {}", schemaRoot.getEntityType());
      } catch (Exception e) {
        if (e instanceof YamlSchemaException) {
          throw e;
        }
        log.info("No schema found for unit testing for {}.", schemaRoot.getEntityType());
      }
    }
  }

  void ensureOneOfHasCorrectValues(Reflections reflections) {
    final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(OneOfField.class, true);
    for (Class<?> clazz : typesAnnotatedWith) {
      final OneOfField annotation = clazz.getAnnotation(OneOfField.class);
      validateForOneOfFields(clazz, annotation);
    }
    final Set<Class<?>> typesAnnotated = reflections.getTypesAnnotatedWith(OneOfFields.class, true);
    for (Class<?> clazz : typesAnnotated) {
      final OneOfFields annotation = clazz.getAnnotation(OneOfFields.class);
      final OneOfField[] value = annotation.value();
      for (OneOfField oneOfField : value) {
        validateForOneOfFields(clazz, oneOfField);
      }
    }
  }

  void validateForOneOfFields(Class<?> clazz, OneOfField annotation) {
    final String[] fields = annotation.fields();
    final Field[] declaredFieldsInClass = clazz.getDeclaredFields();
    final Set<String> decFieldSwaggerName =
        Arrays.stream(declaredFieldsInClass).map(YamlSchemaUtils::getFieldName).collect(Collectors.toSet());
    for (String field : fields) {
      if (!decFieldSwaggerName.contains(field)) {
        throw new InvalidRequestException(String.format("Field %s has incorrect Name", field));
      }
      log.info("One of field passed for field {}", field);
    }
  }
}
