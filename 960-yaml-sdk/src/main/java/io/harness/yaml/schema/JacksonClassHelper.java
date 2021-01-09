package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.validation.OneOfField;
import io.harness.validation.OneOfFields;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.OneOfMapping;
import io.harness.yaml.schema.beans.PossibleFieldTypes;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.annotations.VisibleForTesting;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JacksonClassHelper {
  /**
   * @param clazz                         Class which will be traversed.
   * @param swaggerDefinitionsMetaInfoMap The map which will be populated with all metainfo needed to be added to
   *     swagger spec.
   */
  public void getRequiredMappings(
      Class<?> clazz, Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap) {
    if (swaggerDefinitionsMetaInfoMap.containsKey(YamlSchemaUtils.getSwaggerName(clazz))) {
      return;
    }
    Set<FieldSubtypeData> fieldSubtypeDataList = new HashSet<>();
    Set<PossibleFieldTypes> possibleFieldTypesSet = new HashSet<>();
    // Instantiating so that we don't get into infinite loop.
    swaggerDefinitionsMetaInfoMap.put(YamlSchemaUtils.getSwaggerName(clazz), null);
    for (Field declaredField : clazz.getDeclaredFields()) {
      if (checkIfClassShouldBeTraveresed(declaredField)) {
        getRequiredMappings(declaredField.getType(), swaggerDefinitionsMetaInfoMap);
      }
      // Field types
      processFieldTypeSet(possibleFieldTypesSet, declaredField);
      // subtype mappings
      processSubtypeMappings(swaggerDefinitionsMetaInfoMap, fieldSubtypeDataList, declaredField);
    }
    // One of mappings
    final Set<OneOfMapping> oneOfMappingForClasses = getOneOfMappingsForClass(clazz);
    final SwaggerDefinitionsMetaInfo definitionsMetaInfo = SwaggerDefinitionsMetaInfo.builder()
                                                               .oneOfMappings(oneOfMappingForClasses)
                                                               .subtypeClassMap(fieldSubtypeDataList)
                                                               .fieldPossibleTypes(possibleFieldTypesSet)
                                                               .build();
    swaggerDefinitionsMetaInfoMap.put(YamlSchemaUtils.getSwaggerName(clazz), definitionsMetaInfo);
  }

  private void processFieldTypeSet(Set<PossibleFieldTypes> possibleFieldTypesSet, Field declaredField) {
    if (declaredField.getAnnotation(YamlSchemaTypes.class) != null) {
      YamlSchemaTypes fieldTypes = declaredField.getAnnotation(YamlSchemaTypes.class);
      Set<SupportedPossibleFieldTypes> value = Arrays.stream(fieldTypes.value()).collect(Collectors.toSet());
      final SupportedPossibleFieldTypes defaultType = fieldTypes.defaultType();
      final String fieldName = YamlSchemaUtils.getFieldName(declaredField);
      possibleFieldTypesSet.add(
          PossibleFieldTypes.builder().fieldName(fieldName).defaultFieldType(defaultType).fieldTypes(value).build());
    }
  }

  private void processSubtypeMappings(Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap,
      Set<FieldSubtypeData> fieldSubtypeDataList, Field declaredField) {
    final Set<SubtypeClassMap> mapOfSubtypes = getMapOfSubtypes(declaredField);
    if (!isEmpty(mapOfSubtypes)) {
      for (JsonSubTypes.Type subtype : Objects.requireNonNull(getJsonSubTypes(declaredField)).value()) {
        getRequiredMappings(subtype.value(), swaggerDefinitionsMetaInfoMap);
      }
      FieldSubtypeData fieldSubtypeData = getFieldSubtypeData(declaredField, mapOfSubtypes);
      fieldSubtypeDataList.add(fieldSubtypeData);
    }
  }

  @VisibleForTesting
  Set<OneOfMapping> getOneOfMappingsForClass(Class<?> clazz) {
    final OneOfField oneOfField = clazz.getAnnotation(OneOfField.class);
    if (oneOfField != null) {
      return Collections.singleton(getOneOfMappingsOfASet(oneOfField, clazz));
    }
    final OneOfFields oneOfFields = clazz.getAnnotation(OneOfFields.class);
    if (oneOfFields != null) {
      return Arrays.stream(oneOfFields.value())
          .map(field -> getOneOfMappingsOfASet(field, clazz))
          .collect(Collectors.toSet());
    }
    return null;
  }

  private OneOfMapping getOneOfMappingsOfASet(OneOfField oneOfField, Class<?> clazz) {
    final Set<String> fields = Arrays.stream(oneOfField.fields()).collect(Collectors.toSet());
    final Field[] declaredFields = clazz.getDeclaredFields();
    final Set<String> oneOfFields = Arrays.stream(declaredFields)
                                        .map(field -> {
                                          if (fields.contains(field.getName())) {
                                            return YamlSchemaUtils.getFieldName(field);
                                          }
                                          return null;
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());
    return OneOfMapping.builder().oneOfFieldNames(oneOfFields).nullable(oneOfField.nullable()).build();
  }

  private FieldSubtypeData getFieldSubtypeData(Field declaredField, Set<SubtypeClassMap> mapOfSubtypes) {
    final JsonTypeInfo annotation = declaredField.getAnnotation(JsonTypeInfo.class);
    final JsonTypeInfo.As include = annotation.include();
    return FieldSubtypeData.builder()
        .fieldName(YamlSchemaUtils.getFieldName(declaredField))
        .subtypesMapping(mapOfSubtypes)
        .discriminatorType(include)
        .discriminatorName(getDiscriminator(declaredField))
        .build();
  }

  private boolean checkIfClassShouldBeTraveresed(Field declaredField) {
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
