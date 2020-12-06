package software.wings.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;

public class JsonUtils {
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    OBJECT_MAPPER.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static <T> T readResourceFile(String filPath, Class<T> tClass) {
    File file = new File(JsonUtils.class.getClassLoader().getResource(filPath).getFile());
    try {
      return OBJECT_MAPPER.readValue(file, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
  public static <T> T readResourceFile(String filPath, TypeReference<T> tClass) {
    File file = new File(JsonUtils.class.getClassLoader().getResource(filPath).getFile());
    try {
      return OBJECT_MAPPER.readValue(file, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T convertJsonNodeToObj(JsonNode jsonNode, TypeReference<T> tClass) {
    return OBJECT_MAPPER.convertValue(jsonNode, tClass);
  }

  public static JsonNode toJsonNode(Object obj) {
    return OBJECT_MAPPER.convertValue(obj, JsonNode.class);
  }
}
