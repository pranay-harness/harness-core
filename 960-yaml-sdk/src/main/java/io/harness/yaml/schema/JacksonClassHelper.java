package io.harness.yaml.schema;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

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
import io.swagger.annotations.ApiModelProperty;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonClassHelper {
  /**
   * @param clazz                         Class which will be traversed.
   * @param swaggerDefinitionsMetaInfoMap The map which will be populated with all metainfo needed to be added to
   *     swagger spec.
   */
  public void getRequiredMappings(
      Class<?> clazz, Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap) {
    String swaggerClassName = YamlSchemaUtils.getSwaggerName(clazz);
    if (swaggerDefinitionsMetaInfoMap.containsKey(swaggerClassName)) {
      return;
    }
    Set<FieldSubtypeData> fieldSubtypeDataList = new HashSet<>();
    Set<PossibleFieldTypes> possibleFieldTypesSet = new HashSet<>();
    // Instantiating so that we don't get into infinite loop.
    swaggerDefinitionsMetaInfoMap.put(swaggerClassName, null);

    for (Field declaredField : clazz.getDeclaredFields()) {
      if (checkIfClassShouldBeTraversed(declaredField)) {
        getRequiredMappings(declaredField.getType(), swaggerDefinitionsMetaInfoMap);
      }
      if (checkIfClassIsCollection(declaredField)) {
        ParameterizedType collectionType = (ParameterizedType) declaredField.getGenericType();
        Class<?> collectionTypeClass = (Class<?>) collectionType.getActualTypeArguments()[0];
        getRequiredMappings(collectionTypeClass, swaggerDefinitionsMetaInfoMap);
      }
      Class<?> aClass = getAlternativeClassType(declaredField);
      if (aClass != null) {
        getRequiredMappings(aClass, swaggerDefinitionsMetaInfoMap);
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
    swaggerDefinitionsMetaInfoMap.put(swaggerClassName, definitionsMetaInfo);
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
    Set<SubtypeClassMap> mapOfSubtypes = getMapOfSubtypes(declaredField);
    if (isEmpty(mapOfSubtypes)) {
      return;
    }
    for (SubtypeClassMap subtype : mapOfSubtypes) {
      getRequiredMappings(subtype.getSubTypeClass(), swaggerDefinitionsMetaInfoMap);
    }
    // Subtype mappings.
    FieldSubtypeData fieldSubtypeData = YamlSchemaUtils.getFieldSubtypeData(declaredField, mapOfSubtypes);
    if (fieldSubtypeData != null) {
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

  private Class<?> getAlternativeClassType(Field declaredField) {
    ApiModelProperty annotation = declaredField.getAnnotation(ApiModelProperty.class);
    if (annotation != null && !annotation.hidden()) {
      String dataType = annotation.dataType();
      try {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return Class.forName(dataType, false, contextClassLoader);
      } catch (ClassNotFoundException e) {
        log.debug(format("dataType: %s is defined in different module, outside of class path", dataType), e);
        // class is defined in different module, outside of this modules class path
        return null;
      }
    }
    return null;
  }

  private boolean checkIfClassShouldBeTraversed(Field declaredField) {
    // Generating only for harness classes hence checking if package is software.wings or io.harness.
    return !declaredField.getType().isPrimitive() && !declaredField.getType().isEnum()
        && (declaredField.getType().getCanonicalName().startsWith("io.harness")
            || declaredField.getType().getCanonicalName().startsWith("software.wings"));
  }

  private Set<SubtypeClassMap> getMapOfSubtypes(Field field) {
    JsonSubTypes annotation = YamlSchemaUtils.getJsonSubTypes(field);
    if (annotation == null) {
      return null;
    }
    return Arrays.stream(annotation.value())
        .filter(Objects::nonNull)
        .map(jsonSubType
            -> SubtypeClassMap.builder()
                   .subtypeEnum(jsonSubType.name())
                   .subTypeDefinitionKey(YamlSchemaUtils.getSwaggerName(jsonSubType.value()))
                   .subTypeClass(jsonSubType.value())
                   .build())
        .collect(Collectors.toSet());
  }

  /**
   * @param field field in the java POJO for which discriminator is required.
   * @return the value of field which helps in discriminating field's subtypes.
   */
  private String getDiscriminator(Field field) {
    JsonTypeInfo jsonTypeInfo = YamlSchemaUtils.getJsonTypeInfo(field);
    if (jsonTypeInfo != null) {
      return jsonTypeInfo.property();
    }
    return null;
  }

  private boolean checkIfClassIsCollection(Field declaredField) {
    return Collection.class.isAssignableFrom(declaredField.getType());
  }
}
