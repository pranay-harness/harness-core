/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.validation.OneOfField;
import io.harness.validation.OneOfFields;
import io.harness.validation.OneOfSet;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.OneOfMapping;
import io.harness.yaml.schema.beans.OneOfSetMapping;
import io.harness.yaml.schema.beans.PossibleFieldTypes;
import io.harness.yaml.schema.beans.StringFieldTypeMetadata;
import io.harness.yaml.schema.beans.SubtypeClassMap;
import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;
import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
@Singleton
@OwnedBy(DX)
public class JacksonClassHelper {
  @Inject
  public JacksonClassHelper(@Named("yaml-schema-mapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  ObjectMapper objectMapper;

  /**
   * @param aClass                        Class which will be traversed.
   * @param swaggerDefinitionsMetaInfoMap The map which will be populated with all metainfo needed to be added to
   *                                      swagger spec.
   */
  public void getRequiredMappings(
      Class<?> aClass, Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap) {
    List<Class<?>> classes = new ArrayList<>();
    classes.add(aClass);
    ApiModel annotation = aClass.getAnnotation(ApiModel.class);
    if (annotation != null && isNotEmpty(annotation.subTypes())) {
      Collections.addAll(classes, annotation.subTypes());
    }

    for (Class<?> clazz : classes) {
      String swaggerClassName = YamlSchemaUtils.getSwaggerName(clazz);
      if (swaggerDefinitionsMetaInfoMap.containsKey(swaggerClassName)) {
        return;
      }
      Set<FieldSubtypeData> fieldSubtypeDataList = new HashSet<>();
      Set<PossibleFieldTypes> possibleFieldTypesSet = new HashSet<>();
      Set<String> nonEmptyFields = new HashSet<>();

      // Instantiating so that we don't get into infinite loop.
      swaggerDefinitionsMetaInfoMap.put(swaggerClassName, null);

      for (Field declaredField : FieldUtils.getAllFields(clazz)) {
        if (YamlSchemaUtils.checkIfClassShouldBeTraversed(declaredField)) {
          getRequiredMappings(declaredField.getType(), swaggerDefinitionsMetaInfoMap);
        }

        if (YamlSchemaUtils.checkIfClassIsCollection(declaredField)) {
          ParameterizedType collectionType = (ParameterizedType) declaredField.getGenericType();
          Class<?> collectionTypeClass = (Class<?>) collectionType.getActualTypeArguments()[0];
          getRequiredMappings(collectionTypeClass, swaggerDefinitionsMetaInfoMap);
        }
        Class<?> alternativeClass = getAlternativeClassType(declaredField);
        if (alternativeClass != null) {
          getRequiredMappings(alternativeClass, swaggerDefinitionsMetaInfoMap);
        }
        // Field types
        processFieldTypeSet(possibleFieldTypesSet, declaredField);
        // subtype mappings
        processSubtypeMappings(swaggerDefinitionsMetaInfoMap, fieldSubtypeDataList, declaredField);
        // Non empty fields
        processNonEmptyFields(nonEmptyFields, declaredField);
      }
      // One of mappings
      final Set<OneOfMapping> oneOfMappingForClass = getOneOfMappingsForClass(clazz);
      // One of set mappings
      final OneOfSetMapping oneOfSetMappingForClass = getOneOfSetMappingsForClass(clazz);
      if (isNotEmpty(oneOfMappingForClass) && oneOfSetMappingForClass != null) {
        throw new InvalidRequestException(
            String.format("Class %s cannot have both OneOfField and OneOfSet annotation", swaggerClassName));
      }
      checkIfSubTypeDataListIsNotUnique(fieldSubtypeDataList);
      final SwaggerDefinitionsMetaInfo definitionsMetaInfo = SwaggerDefinitionsMetaInfo.builder()
                                                                 .oneOfMappings(oneOfMappingForClass)
                                                                 .subtypeClassMap(fieldSubtypeDataList)
                                                                 .fieldPossibleTypes(possibleFieldTypesSet)
                                                                 .notEmptyStringFields(nonEmptyFields)
                                                                 .oneOfSetMapping(oneOfSetMappingForClass)
                                                                 .build();
      swaggerDefinitionsMetaInfoMap.put(swaggerClassName, definitionsMetaInfo);
    }
  }

  private void processNonEmptyFields(Set<String> nonEmptyFields, Field declaredField) {
    if (declaredField.getAnnotation(NotEmpty.class) != null) {
      nonEmptyFields.add(YamlSchemaUtils.getFieldName(declaredField));
    }
  }

  private void checkIfSubTypeDataListIsNotUnique(Set<FieldSubtypeData> fieldSubtypeData) {
    Map<String, Set<String>> fieldSubtypeEnumMap = new HashMap<>();
    for (FieldSubtypeData fieldSubtypeDatum : fieldSubtypeData) {
      final String fieldName = fieldSubtypeDatum.getFieldName();
      if (!fieldSubtypeEnumMap.containsKey(fieldName)) {
        final Set<SubtypeClassMap> subtypesMapping = fieldSubtypeDatum.getSubtypesMapping();
        fieldSubtypeEnumMap.put(fieldName, new HashSet<>());
        for (SubtypeClassMap subtypeClassMap : subtypesMapping) {
          final Set<String> enums = fieldSubtypeEnumMap.get(fieldName);
          if (enums.contains(subtypeClassMap.getSubtypeEnum())) {
            throw new InvalidRequestException(
                String.format("Enum %s being generated twice in yaml schema. ", subtypeClassMap.getSubtypeEnum()));
          } else {
            enums.add(subtypeClassMap.getSubtypeEnum());
          }
        }
      } else {
        throw new InvalidRequestException(
            String.format("field %s being generated twice in yaml schema. ", fieldSubtypeDatum));
      }
    }
  }

  private List<Class<?>> getSubtypesOfClass(Class<?> clazz) {
    List<Class<?>> classes = new ArrayList<>();
    MapperConfig<?> config = objectMapper.getDeserializationConfig();
    AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(clazz, config);
    final Collection<NamedType> namedTypes =
        objectMapper.getSubtypeResolver().collectAndResolveSubtypesByClass(config, ac);
    if (!namedTypes.isEmpty()) {
      for (NamedType namedType : namedTypes) {
        classes.add(namedType.getType());
      }
    }
    return classes;
  }

  private void processFieldTypeSet(Set<PossibleFieldTypes> possibleFieldTypesSet, Field declaredField) {
    if (declaredField.getAnnotation(YamlSchemaTypes.class) != null) {
      YamlSchemaTypes fieldTypes = declaredField.getAnnotation(YamlSchemaTypes.class);
      Set<SupportedPossibleFieldTypes> value = Arrays.stream(fieldTypes.value()).collect(Collectors.toSet());
      final SupportedPossibleFieldTypes defaultType = fieldTypes.defaultType();
      final String fieldName = YamlSchemaUtils.getFieldName(declaredField);
      possibleFieldTypesSet.add(PossibleFieldTypes.builder()
                                    .fieldName(fieldName)
                                    .defaultFieldType(defaultType)
                                    .fieldTypes(value)
                                    .fieldTypesMetadata(StringFieldTypeMetadata.builder()
                                                            .minLength(fieldTypes.minLength())
                                                            .pattern(fieldTypes.pattern())
                                                            .build())
                                    .build());
    }
  }

  private void processSubtypeMappings(Map<String, SwaggerDefinitionsMetaInfo> swaggerDefinitionsMetaInfoMap,
      Set<FieldSubtypeData> fieldSubtypeDataList, Field declaredField) {
    Set<SubtypeClassMap> mapOfSubtypesUsingJackson = getMapOfSubtypes(declaredField);
    final Set<SubtypeClassMap> mapOfSubtypesUsingReflection =
        YamlSchemaUtils.getMapOfSubtypesUsingObjectMapper(declaredField, objectMapper);
    Set<SubtypeClassMap> mapOfSubtypes = new HashSet<>();
    if (!isEmpty(mapOfSubtypesUsingJackson)) {
      mapOfSubtypes.addAll(mapOfSubtypesUsingJackson);
    }
    if (!isEmpty(mapOfSubtypesUsingReflection)) {
      mapOfSubtypes.addAll(mapOfSubtypesUsingReflection);
    }
    if (isEmpty(mapOfSubtypes)) {
      return;
    }
    for (SubtypeClassMap subtype : mapOfSubtypes) {
      getRequiredMappings(subtype.getSubTypeClass(), swaggerDefinitionsMetaInfoMap);
    }
    // Subtype mappings.
    FieldSubtypeData fieldSubtypeData = YamlSchemaUtils.getFieldSubtypeData(declaredField, mapOfSubtypesUsingJackson);
    if (fieldSubtypeData != null) {
      fieldSubtypeDataList.add(fieldSubtypeData);
    }
    final FieldSubtypeData fieldSubtypeDataUsingReflection =
        YamlSchemaUtils.getFieldSubtypeData(declaredField, mapOfSubtypesUsingReflection);
    if (fieldSubtypeDataUsingReflection != null) {
      fieldSubtypeDataList.add(fieldSubtypeDataUsingReflection);
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

  @VisibleForTesting
  OneOfSetMapping getOneOfSetMappingsForClass(Class<?> clazz) {
    final OneOfSet oneOfSetAnnotation = clazz.getAnnotation(OneOfSet.class);
    if (oneOfSetAnnotation != null) {
      final Field[] declaredFields = clazz.getDeclaredFields();
      Set<Set<String>> mappedOneOfSetFields = getMappedOneOfSetFields(oneOfSetAnnotation, declaredFields);
      Set<String> requiredFieldNames = getRequiredFieldNames(oneOfSetAnnotation, declaredFields);
      return OneOfSetMapping.builder().oneOfSets(mappedOneOfSetFields).requiredFieldNames(requiredFieldNames).build();
    }
    return null;
  }

  private Set<String> getRequiredFieldNames(OneOfSet oneOfSetAnnotation, Field[] declaredFields) {
    return Arrays.stream(oneOfSetAnnotation.requiredFieldNames())
        .map(requiredFieldName -> {
          Optional<Field> declaredField =
              Arrays.stream(declaredFields).filter(f -> requiredFieldName.equals(f.getName())).findFirst();
          return declaredField.map(YamlSchemaUtils::getFieldName).orElse(null);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Set<Set<String>> getMappedOneOfSetFields(OneOfSet oneOfSetAnnotation, Field[] declaredFields) {
    Set<Set<String>> mappedOneOfSetFields = new HashSet<>();
    for (String oneOfSetFields : oneOfSetAnnotation.fields()) {
      Set<String> oneOfSet =
          Stream.of(oneOfSetFields.trim().split("\\s*,\\s*"))
              .map(oneOfSetField -> {
                Optional<Field> declaredField =
                    Arrays.stream(declaredFields).filter(f -> oneOfSetField.equals(f.getName())).findFirst();
                return declaredField.map(YamlSchemaUtils::getFieldName).orElse(null);
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      if (isNotEmpty(oneOfSet)) {
        mappedOneOfSetFields.add(oneOfSet);
      }
    }
    return mappedOneOfSetFields;
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
}
