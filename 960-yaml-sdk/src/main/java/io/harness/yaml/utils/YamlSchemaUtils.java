package io.harness.yaml.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.packages.HarnessPackages;
import io.harness.yaml.schema.YamlSchemaRoot;
import io.harness.yaml.schema.beans.FieldSubtypeData;
import io.harness.yaml.schema.beans.SubtypeClassMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

@UtilityClass
public class YamlSchemaUtils {
  /**
   * @param classLoader     {@link ClassLoader} object which will be used for reflection. If null default class loader
   *                        will be used.
   * @param annotationClass Annotation for which lookup will happen.
   * @return Classes which contains the annotation.
   */
  public Set<Class<?>> getClasses(@Nullable URLClassLoader classLoader, Class annotationClass) {
    Reflections reflections;
    if (classLoader != null) {
      FilterBuilder filter = new FilterBuilder().include(FilterBuilder.prefix("io.harness")).include("software.wings");
      reflections = new Reflections(new ConfigurationBuilder()
                                        .forPackages(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS)
                                        .filterInputsBy(filter)
                                        .setUrls(classLoader.getURLs())
                                        .addClassLoader(classLoader));

    } else {
      reflections = new Reflections(HarnessPackages.IO_HARNESS, HarnessPackages.SOFTWARE_WINGS);
    }
    return reflections.getTypesAnnotatedWith(annotationClass, true);
  }

  public Set<Class<?>> getClasses(Class annotationClass) {
    return getClasses(null, annotationClass);
  }

  /**
   * @param clazz The class.
   * @return Name of the class in the swagger doc.
   */
  public String getSwaggerName(Class<?> clazz) {
    try {
      ApiModel declaredAnnotation = clazz.getDeclaredAnnotation(ApiModel.class);
      if (!isEmpty(declaredAnnotation.value())) {
        return declaredAnnotation.value();
      }
    } catch (NullPointerException e) {
      // do Nothing.
    }
    return clazz.getSimpleName();
  }

  /**
   * @param entityType     Entity type
   * @param schemaBasePath the base path inside which schema is stored.
   * @return The path which contains the complete schema for entityType.
   */
  public String getSchemaPathForEntityType(EntityType entityType, String schemaBasePath) {
    final String yamlName = entityType.getYamlName();
    String resourcePath = yamlName + File.separator + YamlConstants.SCHEMA_FILE_NAME;
    return isEmpty(schemaBasePath) ? resourcePath : schemaBasePath + File.separator + resourcePath;
  }

  /**
   * @param clazz     Class
   * @param snippetBasePath the base path inside which schema index is stored.
   * @param snippetIndexFile the index file name.
   * @return The path which contains the complete schema for entityType.
   */
  public String getSnippetIndexPathForEntityType(Class<?> clazz, String snippetBasePath, String snippetIndexFile) {
    String entityName = getEntityName(clazz);
    return snippetBasePath + File.separator + entityName + File.separator + snippetIndexFile;
  }

  /**
   * @param clazz The class.
   * @return EntityType of the class.
   */
  public String getEntityName(Class<?> clazz) {
    YamlSchemaRoot declaredAnnotation = clazz.getDeclaredAnnotation(YamlSchemaRoot.class);
    return declaredAnnotation.value().getYamlName();
  }

  /**
   * @param field the field for which we need json schema value.
   * @return the value of field.
   */
  public String getFieldName(Field field) {
    if (field.getAnnotation(ApiModelProperty.class) != null
        && isNotEmpty(field.getAnnotation(ApiModelProperty.class).name())) {
      return field.getAnnotation(ApiModelProperty.class).name();
    }
    if (field.getAnnotation(JsonProperty.class) != null) {
      return field.getAnnotation(JsonProperty.class).value();
    }
    return field.getName();
  }

  public Field getTypedField(Class<?> aClass) {
    for (Field declaredField : aClass.getDeclaredFields()) {
      JsonTypeInfo jsonTypeInfo = getJsonTypeInfo(declaredField);
      if (jsonTypeInfo != null) {
        return declaredField;
      }
    }
    return null;
  }

  public FieldSubtypeData getFieldSubtypeData(Field typedField, Set<SubtypeClassMap> subtypeClassMaps) {
    JsonTypeInfo annotation = getJsonTypeInfo(typedField);
    if (annotation == null) {
      return null;
    }
    return FieldSubtypeData.builder()
        .fieldName(getFieldName(typedField))
        .discriminatorType(annotation.include())
        .discriminatorName(annotation.property())
        .subtypesMapping(subtypeClassMaps)
        .build();
  }

  public Set<SubtypeClassMap> getMapOfSubtypesUsingReflection(Field field) {
    JsonTypeInfo jsonTypeInfo = getJsonTypeInfo(field);
    if (jsonTypeInfo == null) {
      return null;
    }
    Reflections reflections = new Reflections("io.harness");
    Set<Class<?>> subTypesOf = reflections.getSubTypesOf((Class<Object>) field.getType());
    return subTypesOf.stream()
        .filter(c -> c.getAnnotation(JsonTypeName.class) != null)
        .map(aClass
            -> SubtypeClassMap.builder()
                   .subtypeEnum(aClass.getAnnotation(JsonTypeName.class).value())
                   .subTypeDefinitionKey(io.harness.yaml.utils.YamlSchemaUtils.getSwaggerName(aClass))
                   .subTypeClass(aClass)
                   .build())
        .collect(Collectors.toSet());
  }

  /**
   * @param field field for which subtypes are required, this method looks for JsonSubTypes annotation in field
   *     annotations than in filed's class annotations
   * @return JsonSubTypes annotation
   */
  public JsonSubTypes getJsonSubTypes(Field field) {
    JsonSubTypes annotation = field.getAnnotation(JsonSubTypes.class);
    if (annotation == null || isEmpty(annotation.value())) {
      annotation = field.getType().getAnnotation(JsonSubTypes.class);
    }
    if (annotation == null || isEmpty(annotation.value())) {
      return null;
    }
    return annotation;
  }

  public JsonTypeInfo getJsonTypeInfo(Field field) {
    JsonTypeInfo annotation = field.getAnnotation(JsonTypeInfo.class);
    if (annotation == null) {
      annotation = field.getType().getAnnotation(JsonTypeInfo.class);
    }
    if (annotation == null) {
      return null;
    }
    return annotation;
  }
}
