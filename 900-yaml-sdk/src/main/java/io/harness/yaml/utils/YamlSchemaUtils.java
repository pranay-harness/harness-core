package io.harness.yaml.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.yamlSchema.YamlSchemaRoot;

import io.swagger.annotations.ApiModel;
import java.io.File;
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
                                        .forPackages("io.harness", "software.wings")
                                        .filterInputsBy(filter)
                                        .setUrls(classLoader.getURLs())
                                        .addClassLoader(classLoader));

    } else {
      reflections = new Reflections();
    }
    return reflections.getTypesAnnotatedWith(annotationClass);
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
   * @param clazz The class.
   * @return EntityType of the class.
   */
  public String getEntityName(Class<?> clazz) {
    YamlSchemaRoot declaredAnnotation = clazz.getDeclaredAnnotation(YamlSchemaRoot.class);
    return declaredAnnotation.value().getYamlName();
  }
}
