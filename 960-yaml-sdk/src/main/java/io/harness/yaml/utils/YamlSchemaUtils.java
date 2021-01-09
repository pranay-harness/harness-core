package io.harness.yaml.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.packages.HarnessPackages;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Set;
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
}
