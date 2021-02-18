package io.harness.serializer.recaster;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonObjectRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  private final ObjectMapper objectMapper;
  public JsonObjectRecastTransformer() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    try {
      if (fromObject == null) {
        return NullNode.getInstance();
      }

      if (targetClass.isAssignableFrom(ShortNode.class)
          && (fromObject.getClass().isAssignableFrom(Short.class)
              || fromObject.getClass().isAssignableFrom(short.class))) {
        return ShortNode.valueOf((Short) fromObject);
      }
      return objectMapper.valueToTree(fromObject);
    } catch (Exception e) {
      log.error("Exception while decoding JsonNode {}", fromObject, e);
      throw e;
    }
  }

  /**
   * Here <code>value</code> could be of type JsonObject or JsonArray, so we need to convert it to object
   * <br>
   * After conversion:
   * <br>
   * &emsp;JsonObject -> LinkedHashMap
   * <br>
   * &emsp;JsonArray  -> ArrayList
   */
  @Override
  public Object encode(Object value, CastedField castedField) {
    try {
      return objectMapper.convertValue(value, Object.class);
    } catch (Exception e) {
      log.error("Exception while encoding JsonNode {}", value, e);
      throw e;
    }
  }

  @Override
  public boolean isSupported(Class<?> c, CastedField cf) {
    return RecastReflectionUtils.implementsInterface(c, JsonNode.class);
  }
}
