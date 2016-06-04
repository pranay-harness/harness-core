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

// TODO: Auto-generated Javadoc

/**
 * The Class YamlUtils.
 *
 * @author Rishi
 */
@Singleton
public class YamlUtils {
  private final ObjectMapper mapper;

  /**
   * Instantiates a new yaml utils.
   */
  public YamlUtils() {
    mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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
  public <T> T read(String yaml, Class<T> cls) throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(yaml, cls);
  }

  /**
   * Read.
   *
   * @param <T>           the generic type
   * @param yaml          the yaml
   * @param typeReference the type reference
   * @return the t
   * @throws JsonParseException   the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException          Signals that an I/O exception has occurred.
   */
  public <T> T read(String yaml, TypeReference<T> typeReference)
      throws JsonParseException, JsonMappingException, IOException {
    return mapper.readValue(yaml, typeReference);
  }
}
