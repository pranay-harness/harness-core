package software.wings.utils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.reinert.jjschema.v1.JsonSchemaFactory;
import com.github.reinert.jjschema.v1.JsonSchemaV4Factory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

/**
 * The Class JsonUtils.
 */
public class JsonUtils {
  /**
   * The constant mapperForCloning.
   */
  public static final ObjectMapper mapperForCloning;
  public static final ObjectMapper mapperForInternalUse;
  private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper mapper;

  static {
    // json-path initialization
    Configuration.setDefaults(new Configuration.Defaults() {
      private final JsonProvider jsonProvider = new JacksonJsonProvider();
      private final MappingProvider mappingProvider = new JacksonMappingProvider();

      @Override
      public JsonProvider jsonProvider() {
        return jsonProvider;
      }

      @Override
      public MappingProvider mappingProvider() {
        return mappingProvider;
      }

      @Override
      public Set<Option> options() {
        return EnumSet.noneOf(Option.class);
      }
    });

    mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setSubtypeResolver(new JsonSubtypeResolver(mapper.getSubtypeResolver()));
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());

    mapperForCloning = new ObjectMapper();
    mapperForCloning.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapperForCloning.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapperForCloning.setSerializationInclusion(Include.NON_NULL);
    mapperForCloning.enableDefaultTyping();
    mapperForCloning.setSubtypeResolver(new JsonSubtypeResolver(mapperForCloning.getSubtypeResolver()));
    mapperForCloning.registerModule(new Jdk8Module());
    mapperForCloning.registerModule(new GuavaModule());
    mapperForCloning.registerModule(new JavaTimeModule());

    mapperForInternalUse = new ObjectMapper();
    mapperForInternalUse.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapperForInternalUse.setSerializationInclusion(Include.NON_NULL);
    mapperForInternalUse.enableDefaultTyping();
    mapperForInternalUse.setSubtypeResolver(new JsonSubtypeResolver(mapperForCloning.getSubtypeResolver()));
    mapperForInternalUse.registerModule(new Jdk8Module());
    mapperForInternalUse.registerModule(new GuavaModule());
    mapperForInternalUse.registerModule(new JavaTimeModule());
  }

  /**
   * Parses the json.
   *
   * @param json the json
   * @return the document context
   */
  public static DocumentContext parseJson(String json) {
    return JsonPath.parse(json);
  }

  /**
   * Json path.
   *
   * @param <T>  the generic type
   * @param ctx  the ctx
   * @param path the path
   * @return the t
   */
  public static <T> T jsonPath(DocumentContext ctx, String path) {
    return ctx.read(path);
  }

  /**
   * Json path.
   *
   * @param <T>  the generic type
   * @param ctx  the ctx
   * @param path the path
   * @param cls  the cls
   * @return the t
   */
  public static <T> T jsonPath(DocumentContext ctx, String path, Class<T> cls) {
    return ctx.read(path, cls);
  }

  /**
   * Json path.
   *
   * @param <T>  the generic type
   * @param json the json
   * @param path the path
   * @return the t
   */
  public static <T> T jsonPath(String json, String path) {
    return JsonPath.read(json, path);
  }

  /**
   * Json path.
   *
   * @param <T>  the generic type
   * @param json the json
   * @param path the path
   * @param cls  the cls
   * @return the t
   */
  public static <T> T jsonPath(String json, String path, Class<T> cls) {
    return JsonPath.parse(json).read(path, cls);
  }

  /**
   * Converts object to json.
   *
   * @param obj Object to be converted.
   * @return json string.
   */
  public static String asJson(Object obj) {
    return asJson(obj, mapper);
  }

  public static String asPrettyJson(Object obj) throws JsonProcessingException {
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  /**
   * As json.
   *
   * @param obj          the obj
   * @param objectMapper the object mapper
   * @return the string
   */
  public static String asJson(Object obj, ObjectMapper objectMapper) {
    try {
      SimpleFilterProvider filterProvider = new SimpleFilterProvider();
      // Do not fail if no filter is set
      filterProvider.setFailOnUnknownId(false);

      // No filters used in this.
      return objectMapper.writer(filterProvider).writeValueAsString(obj);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json string to object of given type.
   *
   * @param <T>            target class type.
   * @param jsonString     json to deserialize.
   * @param classToConvert target class type.
   * @return Deserialized object.
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, Class<T> classToConvert) {
    return asObject(jsonString, classToConvert, mapper);
  }

  /**
   * As object.
   *
   * @param <T>            the generic type
   * @param jsonString     the json string
   * @param classToConvert the class to convert
   * @param objectMapper   the object mapper
   * @return the t
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, Class<T> classToConvert, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(jsonString, classToConvert);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json string to object of given type reference.
   *
   * @param <T>          target class type.
   * @param jsonString   json to deserialize.
   * @param valueTypeRef target class type reference.
   * @return Deserialized object.
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, TypeReference<T> valueTypeRef) {
    return asObject(jsonString, valueTypeRef, mapper);
  }

  /**
   * Deserializes json string to object of given type reference.
   *
   * @param <T>          target class type.
   * @param jsonString   json to deserialize.
   * @param valueTypeRef target class type reference.
   * @param objectMapper the object mapper
   * @return Deserialized object.
   */
  @JsonDeserialize
  public static <T> T asObject(String jsonString, TypeReference<T> valueTypeRef, ObjectMapper objectMapper) {
    try {
      return objectMapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Deserializes json to List of given type.
   *
   * @param <T>          Type of list
   * @param jsonString   json to deserialize.
   * @param valueTypeRef TypeReference for the list.
   * @return deserialized list.
   */
  @JsonDeserialize
  public static <T> T asList(String jsonString, TypeReference<T> valueTypeRef) {
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * validates a json string.
   *
   * @param <T>          collection type.
   * @param jsonString   json to deserialize.
   * @param valueTypeRef target class type.
   * @throws JsonParseException   the json parse exception
   * @throws JsonMappingException the json mapping exception
   * @throws IOException          Signals that an I/O exception has occurred.
   */
  public static <T> void validateJson(String jsonString, TypeReference<T> valueTypeRef)
      throws JsonParseException, JsonMappingException, IOException {
    mapper.readValue(jsonString, valueTypeRef);
  }

  /**
   * Clone.
   *
   * @param <T> the generic type
   * @param t   the t
   * @param cls the cls
   * @return the t
   */
  public static <T> T clone(T t, Class<T> cls) {
    String json = asJson(t, mapperForCloning);
    logger.debug("Cloning Object - json: {}", json);
    return asObject(json, cls, mapperForCloning);
  }

  /**
   * Json schema.
   *
   * @param clazz the clazz
   * @return the json node
   */
  public static JsonNode jsonSchema(Class<?> clazz) {
    return jsonSchema(mapper, clazz);
  }

  /**
   * Json schema.
   *
   * @param objectMapper the object mapper
   * @param clazz        the clazz
   * @return the json node
   */
  public static JsonNode jsonSchema(ObjectMapper objectMapper, Class<?> clazz) {
    JsonSchemaFactory schemaFactory = new JsonSchemaV4Factory();
    return schemaFactory.createSchema(clazz);
  }

  /**
   * Converts object to jsonNode for advanced processing.
   *
   * @param object the object
   * @return the json node
   */
  public static JsonNode asTree(Object object) {
    return asTree(mapper, object);
  }

  /**
   * Converts object to jsonNode for advanced processing.
   *
   * @param objectMapper the object mapper
   * @param object       the object
   * @return the json node
   */
  public static JsonNode asTree(ObjectMapper objectMapper, Object object) {
    return objectMapper.valueToTree(object);
  }

  /**
   * Converts object to jsonNode for advanced processing.
   *
   * @param json string
   * @return the json node
   */
  public static JsonNode readTree(String json) {
    return readTree(mapper, json);
  }

  /**
   * Converts object to jsonNode for advanced processing.
   *
   * @param objectMapper the object mapper
   * @param json         String
   * @return the json node
   */
  public static JsonNode readTree(ObjectMapper objectMapper, String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      for (StackTraceElement elem : e.getStackTrace()) {
        logger.error("Trace: {}", elem);
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Deserializes json string to list of objects of given type.
   *
   * @param <T>            collection type.
   * @param <U>            targetClassType.
   * @param jsonString     json to deserialize.
   * @param collectionType collection type. i.e. List, Set etc.
   * @param classToConvert target class type.
   * @return Deserialized Collection object.
   */
  @JsonDeserialize
  public static <T extends Collection<U>, U> T asObject(
      String jsonString, Class<T> collectionType, Class<U> classToConvert) {
    try {
      return mapper.readValue(
          jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, classToConvert));
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Read resource object.
   *
   * @param file the file
   * @return the object
   */
  public static Object readResource(String file) {
    try {
      URL url = JsonUtils.class.getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new RuntimeException("Error in initializing CommandUnitType-" + file, exception);
    }
  }
}
