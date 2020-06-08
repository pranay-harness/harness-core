package io.harness.yaml.core.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * PropertyBindingPolymorphicDeserializer is custom polymorphic deserializer that can be used in cases to where subtypes
 * do not have property that holds type information, but have unique distinguishable properties.
 * PropertyBindingPolymorphicDeserializer exposes a register method that can be used to register bindings on unique
 * properties.
 * @param <T>
 */
public class PropertyBindingPolymorphicDeserializer<T> extends StdDeserializer<T> {
  private static final long serialVersionUID = 1L;

  // the registry of field name to class bindings
  private final Map<String, Class<? extends T>> bindings;

  public PropertyBindingPolymorphicDeserializer(Class<T> type) {
    super(type);
    bindings = new HashMap<>();
  }

  /***
   * Register bindings that represent unique properties for given type
   * @param propertyName name of unique property
   * @param type class to bind for this unique property
   */
  public void registerBinding(String propertyName, Class<? extends T> type) {
    bindings.put(propertyName, type);
  }

  @Override
  public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    Class<? extends T> type = null;

    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    ObjectNode obj = mapper.readTree(jp);
    Iterator<String> fieldNamesIterator = obj.fieldNames();

    while (fieldNamesIterator.hasNext()) {
      String fieldName = fieldNamesIterator.next();

      if (bindings.containsKey(fieldName)) {
        type = bindings.get(fieldName);
        break;
      }
    }

    if (type == null) {
      throw ctxt.mappingException("No registered binding found for deserialization");
    }

    return mapper.treeToValue(obj, type);
  }
}
