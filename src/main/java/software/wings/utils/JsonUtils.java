package software.wings.utils;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.reinert.jjschema.JsonSchemaGenerator;
import com.github.reinert.jjschema.SchemaGeneratorBuilder;
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

// TODO: Auto-generated Javadoc

/**
 * The Class JsonUtils.
 */
public class JsonUtils {
  private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper mapper;
  private static final ObjectMapper mapperForCloning;

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

    mapperForCloning = new ObjectMapper();
    mapperForCloning.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapperForCloning.setSerializationInclusion(Include.NON_NULL);
    mapperForCloning.enableDefaultTyping();
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
      logger.error(exception.getMessage(), exception);
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
      logger.error(exception.getMessage(), exception);
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
    try {
      return mapper.readValue(jsonString, valueTypeRef);
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
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
      logger.error(exception.getMessage(), exception);
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
    try {
      JsonSchemaGenerator v4generator = SchemaGeneratorBuilder.draftV4Schema().build();
      JsonNode schemaNode = v4generator.generateSchema(clazz);
      return schemaNode;
    } catch (Exception e) {
      logger.error("", e);
      throw Throwables.propagate(e);
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
  public <T extends Collection<U>, U> T asObject(String jsonString, Class<T> collectionType, Class<U> classToConvert) {
    try {
      return mapper.readValue(
          jsonString, mapper.getTypeFactory().constructCollectionType(collectionType, classToConvert));
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
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
    try {
      return objectMapper.valueToTree(object);
    } catch (Exception e) {
      logger.error("", e);
      throw Throwables.propagate(e);
    }
  }
}
