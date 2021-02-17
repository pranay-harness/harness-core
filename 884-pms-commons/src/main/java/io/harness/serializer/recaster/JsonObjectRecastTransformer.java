package io.harness.serializer.recaster;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonObjectRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    try {
      return new ObjectMapper().valueToTree(fromObject);
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
      return new ObjectMapper().convertValue(value, Object.class);
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
