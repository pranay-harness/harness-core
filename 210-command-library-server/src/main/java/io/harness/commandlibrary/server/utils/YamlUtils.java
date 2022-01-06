/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
