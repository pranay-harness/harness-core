/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class SwaggerGenerator {
  private final ObjectMapper objectMapper;

  @Inject
  public SwaggerGenerator(@Named("yaml-schema-mapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

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
