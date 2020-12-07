package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.SubtypeClassMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JacksonSubtypeHelper {
  /**
   * @param clazz            Class and all the descendants for which we need subtypes mapping.
   * @param classSubtypesMap The map which will be populated with subtypes mapping.
   */
  public void getSubtypeMapping(Class<?> clazz, Map<String, Set<FieldSubtypeData>> classSubtypesMap) {
    if (classSubtypesMap.containsKey(YamlSchemaUtils.getSwaggerName(clazz))) {
      return;
    }
    Set<FieldSubtypeData> fieldSubtypeDataList = new HashSet<>();
    classSubtypesMap.put(YamlSchemaUtils.getSwaggerName(clazz), fieldSubtypeDataList);
    for (Field declaredField : clazz.getDeclaredFields()) {
      if (checkIfSubtypeMappingForFieldIsRequired(declaredField)) {
        getSubtypeMapping(declaredField.getType(), classSubtypesMap);
      }
      final Set<SubtypeClassMap> mapOfSubtypes = getMapOfSubtypes(declaredField);
      if (!isEmpty(mapOfSubtypes)) {
        for (JsonSubTypes.Type subtype : Objects.requireNonNull(getJsonSubTypes(declaredField)).value()) {
          getSubtypeMapping(subtype.value(), classSubtypesMap);
        }
        final JsonTypeInfo annotation = declaredField.getAnnotation(JsonTypeInfo.class);
        final JsonTypeInfo.As include = annotation.include();
        FieldSubtypeData fieldSubtypeData = FieldSubtypeData.builder()
                                                .fieldName(getFieldName(declaredField))
                                                .subtypesMapping(mapOfSubtypes)
                                                .discriminatorType(include)
                                                .discriminatorName(getDiscriminator(declaredField))
                                                .build();
        fieldSubtypeDataList.add(fieldSubtypeData);
      }
    }
    classSubtypesMap.put(YamlSchemaUtils.getSwaggerName(clazz), fieldSubtypeDataList);
  }

  private boolean checkIfSubtypeMappingForFieldIsRequired(Field declaredField) {
    // Generating only for harness classes hence checking if package is software.wings or io.harness.
    return !declaredField.getType().isPrimitive() && !declaredField.getType().isEnum()
        && (declaredField.getType().getCanonicalName().startsWith("io.harness")
            || declaredField.getType().getCanonicalName().startsWith("software.wings"));
  }

  private Set<SubtypeClassMap> getMapOfSubtypes(Field field) {
    JsonSubTypes annotation = getJsonSubTypes(field);
    if (annotation == null) {
      return null;
    }
    return Arrays.stream(annotation.value())
        .map(jsonSubType
            -> SubtypeClassMap.builder()
                   .subtypeEnum(jsonSubType.name())
                   .subTypeDefinitionKey(YamlSchemaUtils.getSwaggerName(jsonSubType.value()))
                   .build())
        .collect(Collectors.toSet());
  }

  /**
   * @param field field for which subtypes are required
   * @return
   */
  private JsonSubTypes getJsonSubTypes(Field field) {
    JsonSubTypes annotation = field.getAnnotation(JsonSubTypes.class);
    if (annotation == null || isEmpty(annotation.value())) {
      annotation = field.getType().getAnnotation(JsonSubTypes.class);
    }
    if (annotation == null || isEmpty(annotation.value())) {
      return null;
    }
    return annotation;
  }

  /**
   * @param field the field for which we need json schema value.
   * @return the value of field.
   * TODO: check if ApiModelProperty value is required.
   */
  private String getFieldName(Field field) {
    if (field.getAnnotation(ApiModelProperty.class) != null) {
      return field.getAnnotation(ApiModelProperty.class).value();
    }
    if (field.getAnnotation(JsonProperty.class) != null) {
      return field.getAnnotation(JsonProperty.class).value();
    }
    return field.getName();
  }

  /**
   * @param field field in the java POJO for which discriminator is required.
   * @return the value of field which helps in discriminating field's subtypes.
   */
  private String getDiscriminator(Field field) {
    // explore possibility if it is present over the class and in the declaration.
    if (field.getAnnotation(JsonTypeInfo.class) != null) {
      return field.getAnnotation(JsonTypeInfo.class).property();
    }
    return null;
  }
}
