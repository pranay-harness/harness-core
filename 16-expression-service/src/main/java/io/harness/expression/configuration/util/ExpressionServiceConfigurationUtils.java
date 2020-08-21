package io.harness.expression.configuration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.expression.app.ExpressionServiceConfiguration;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

@UtilityClass
public class ExpressionServiceConfigurationUtils {
  public ExpressionServiceConfiguration getApplicationConfiguration(InputStream config) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    return objectMapper.readValue(config, ExpressionServiceConfiguration.class);
  }
}
