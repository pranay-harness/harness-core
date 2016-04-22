package software.wings.common;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class JsonUtils {
  private static final ObjectMapper mapper;
  private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

  static {
    mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /**
   * Converts object to json.
   * @param obj Object to be converted.
   * @return json string.
   */
  public static String asJson(Object obj) {
    try {
      SimpleFilterProvider filterProvider = new SimpleFilterProvider();
      // Do not fail if no filter is set
      filterProvider.setFailOnUnknownId(false);

      // No filters used in this.
      return mapper.writer(filterProvider).writeValueAsString(obj);
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json string to object of given type.
   * @param jsonString json to deserialize.
   * @param classToConvert target class type.
   * @param <T> target class type.
   * @return Deserialized object.
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, Class<T> classToConvert) {
    try {
      return mapper.readValue(jsonString, classToConvert);
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json string to object of given type reference.
   * @param jsonString json to deserialize.
   * @param valueTypeRef target class type reference.
   * @param <T> target class type.
   * @return Deserialized object.
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json string to list of objects of given type.
   * @param jsonString json to deserialize.
   * @param collectionType collection type. i.e. List, Set etc.
   * @param classToConvert target class type.
   * @param <T> collection type.
   * @param <U> targetClassType.
   * @return Deserialized Collection object.
   */
  @JsonDeserialize
  public static <T extends Collection<U>, U> T asObject(
      String jsonString, Class<T> collectionType, Class<U> classToConvert) {
    try {
      return mapper.readValue(
          jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, classToConvert));
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json to List of given type.
   * @param jsonString json to deserialize.
   * @param valueTypeRef TypeReference for the list.
   * @param <T> Type of list
   * @return deserialized list.
   */
  @JsonDeserialize
  public static <T> T asList(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  /**
   * validates a json string.
   * @param jsonString json to deserialize.
   * @param valueTypeRef target class type.
   * @param <T> collection type.
   */
  public static <T> void validateJson(String jsonString, TypeReference<T> valueTypeRef)
      throws JsonParseException, JsonMappingException, IOException {
    mapper.readValue(jsonString, valueTypeRef);
  }
}
