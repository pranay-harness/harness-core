package io.harness.yaml.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.jackson.HarnessJacksonModule;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.URL;

/**
 * YamlPipelineUtils is used to convert arbitrary class from yaml file.
 * ObjectMapper is preconfigured for yaml parsing. It uses custom
 * {@link JsonSubtypeResolver} which is responsible for scanning entire code base
 * and registering classes that are extending base interfaces or abstract classes defined in this framworkd
 * End user will typically extend interface and define {@link com.fasterxml.jackson.annotation.JsonTypeName} annotation
 * with proper type name. This way framework is decoupled from concrete implementations.
 */
@UtilityClass
public class YamlPipelineUtils {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setSubtypeResolver(new JsonSubtypeResolver(mapper.getSubtypeResolver()));
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new HarnessJacksonModule());
  }

  /**
   * Read.
   *
   * @param <T>  the generic type
   * @param yaml the yaml
   * @param cls  the cls
   * @return the t
   * @throws JsonParseException   the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException          Signals that an I/O exception has occurred.
   */
  public static <T> T read(String yaml, Class<T> cls) throws IOException {
    return mapper.readValue(yaml, cls);
  }

  /**
   * Read.
   *
   * @param <T>  the generic type
   * @param yaml the yaml
   * @param cls  the cls
   * @return the t
   * @throws JsonParseException   the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException          Signals that an I/O exception has occurred.
   */
  public static <T> T read(URL yaml, Class<T> cls) throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(yaml, cls);
  }
}
