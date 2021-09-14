/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.configuration.util;

import io.harness.expression.app.ExpressionServiceConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionServiceConfigurationUtils {
  public ExpressionServiceConfiguration getApplicationConfiguration(InputStream config) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    return objectMapper.readValue(config, ExpressionServiceConfiguration.class);
  }
}
