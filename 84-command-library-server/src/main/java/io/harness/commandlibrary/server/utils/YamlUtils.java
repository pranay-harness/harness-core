package io.harness.commandlibrary.server.utils;

import static com.google.common.base.Suppliers.memoize;
import static io.harness.exception.WingsException.USER_ADMIN;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.harness.exception.YamlException;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.function.Supplier;

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
      throw new YamlException("error while converting", e, USER_ADMIN);
    }
  }
}
