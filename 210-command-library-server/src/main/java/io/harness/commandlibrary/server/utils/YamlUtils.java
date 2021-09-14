/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.utils;

import static io.harness.exception.WingsException.USER_ADMIN;

import static com.google.common.base.Suppliers.memoize;

import io.harness.exception.YamlException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlUtils {
  private final Supplier<ObjectMapper> yamlObjectMappedSupplier = memoize(YamlUtils::createYamlObjectMapper);

  private ObjectMapper createYamlObjectMapper() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }

  public static ObjectMapper getYamlObjectMapper() {
    return yamlObjectMappedSupplier.get();
  }

  public static <T> T fromYaml(String yamlString, Class<T> yamlClass) {
    try {
      return getYamlObjectMapper().readValue(yamlString, yamlClass);
    } catch (IOException e) {
      throw new YamlException("Encountered error" + e.getMessage() + "while parsing YAML.", e, USER_ADMIN);
    }
  }
}
