package software.wings.common;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class JsonUtils {
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public static String asJson(Object obj) {
    try {
      SimpleFilterProvider filterProvider = new SimpleFilterProvider();
      // Do not fail if no filter is set
      filterProvider.setFailOnUnknownId(false);

      // No filters used in this.
      return mapper.writer(filterProvider).writeValueAsString(obj);

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @JsonDeserialize
  public static <T> T asObject(String jsonString, Class<T> classToConvert) {
    try {
      return mapper.readValue(jsonString, classToConvert);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @JsonDeserialize
  public static <T extends Collection<U>, U> T asObject(
      String jsonString, Class<T> collectionType, Class<U> classToConvert) {
    try {
      return mapper.readValue(
          jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, classToConvert));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @JsonDeserialize
  public static <T> T asList(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @JsonDeserialize
  public static <T> T asObject(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public static <T> void validateJson(String jsonString, TypeReference<T> valueTypeRef)
      throws JsonParseException, JsonMappingException, IOException {
    mapper.readValue(jsonString, valueTypeRef);
  }

  private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);
}
