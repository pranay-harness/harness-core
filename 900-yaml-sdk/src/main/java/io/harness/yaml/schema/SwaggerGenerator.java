package io.harness.yaml.schema;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SwaggerGenerator {
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
    final Map<String, Model> models = ModelConverters.getInstance().readAll(type, annotation);
    for (Map.Entry<String, Model> entry : models.entrySet()) {
      swagger.model(entry.getKey(), entry.getValue());
    }
  }
}
