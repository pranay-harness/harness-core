/**
 *
 */
package software.wings.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

import javax.inject.Singleton;

/**
 * @author Rishi
 */
@Singleton
public class YamlUtils {
  private final ObjectMapper mapper;

  public YamlUtils() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public <T> T read(String yaml, Class<T> cls) throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(yaml, cls);
  }

  public <T> T read(String yaml, TypeReference<T> typeReference)
      throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(yaml, typeReference);
  }
}
