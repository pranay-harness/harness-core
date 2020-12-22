package io.harness.pms.serializer.json;

import io.harness.pms.serializer.jackson.NGHarnessJacksonModule;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class JsonOrchestrationUtils {
  public static final ObjectMapper mapper;
  public static final ObjectMapper nonIgnoringMapper;

  static {
    mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setSubtypeResolver(AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver()));
    mapper.registerModule(new ProtobufModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new NGHarnessJacksonModule());
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);

    nonIgnoringMapper = new ObjectMapper();
    nonIgnoringMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    nonIgnoringMapper.setSerializationInclusion(Include.NON_NULL);
    nonIgnoringMapper.setAnnotationIntrospector(new JsonAnnotationIntrospector());
    nonIgnoringMapper.setSubtypeResolver(AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver()));
    nonIgnoringMapper.registerModule(new ProtobufModule());
    nonIgnoringMapper.registerModule(new Jdk8Module());
    nonIgnoringMapper.registerModule(new GuavaModule());
    nonIgnoringMapper.registerModule(new JavaTimeModule());
    nonIgnoringMapper.registerModule(new NGHarnessJacksonModule());
    nonIgnoringMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
  }

  public static String asJson(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static String asJsonWithIgnoredFields(Object obj) {
    try {
      return nonIgnoringMapper.writeValueAsString(obj);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @JsonDeserialize
  public static <T> T asObject(String jsonString, Class<T> classToConvert) {
    try {
      return mapper.readValue(jsonString, classToConvert);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @JsonDeserialize
  public static <T> T asObject(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @JsonDeserialize
  public static Map<String, Object> asMap(String jsonString) {
    try {
      return mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
