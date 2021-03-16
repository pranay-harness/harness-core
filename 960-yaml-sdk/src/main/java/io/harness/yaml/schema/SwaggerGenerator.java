package io.harness.yaml.schema;

import io.harness.yaml.utils.YamlSchemaUtils;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.swagger.converter.ModelConverters;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class SwaggerGenerator {
  @Named("yaml-schema-mapper") ObjectMapper objectMapper;

  /**
   * @param baseClass Class for which and all its descendants swagger definitions will be generated.
   * @return the map of Swagger Definition of classes with key being swagger name{@link
   *     YamlSchemaUtils#getSwaggerName(Class)} of POJOs.
   */
  public Map<String, Model> generateDefinitions(Class<?> baseClass) {
    Swagger swagger = new Swagger();
    populateSwaggerModels(swagger, baseClass, null);
    return swagger.getDefinitions();
  }

  private void populateSwaggerModels(Swagger swagger, Type type, JsonView annotation) {
    final Map<String, Model> models = getModelConverters().readAll(type, annotation);
    for (Map.Entry<String, Model> entry : models.entrySet()) {
      swagger.model(entry.getKey(), entry.getValue());
    }
  }

  private ModelConverters getModelConverters() {
    ModelConverters modelConverters = new ModelConverters();
    modelConverters.addConverter(new ModelResolver(objectMapper));
    return modelConverters;
  }
}
