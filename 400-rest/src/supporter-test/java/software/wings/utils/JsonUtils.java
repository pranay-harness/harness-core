/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {
  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static <T> T readResourceFile(String filPath, Class<T> tClass) {
    InputStream stream = JsonUtils.class.getClassLoader().getResourceAsStream(filPath);
    try {
      return objectMapper.readValue(stream, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T readResourceFile(String filPath, TypeReference<T> tClass) {
    InputStream stream = JsonUtils.class.getClassLoader().getResourceAsStream(filPath);
    try {
      return objectMapper.readValue(stream, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T convertJsonNodeToObj(JsonNode jsonNode, TypeReference<T> tClass) {
    return objectMapper.convertValue(jsonNode, tClass);
  }

  public static <T> T convertStringToObj(String jsonNode, Class<T> tClass) {
    try {
      return objectMapper.readValue(jsonNode, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static JsonNode toJsonNode(Object obj) {
    return objectMapper.convertValue(obj, JsonNode.class);
  }
}
