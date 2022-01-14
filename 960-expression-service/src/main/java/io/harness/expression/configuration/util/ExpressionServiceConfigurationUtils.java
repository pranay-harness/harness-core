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
