package io.harness.serializer.recaster;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class JsonObjectRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    return new ObjectMapper().valueToTree(fromObject);
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return new ObjectMapper().convertValue(value, new TypeReference<Map<String, Object>>() {});
  }

  @Override
  public boolean isSupported(Class<?> c, CastedField cf) {
    return RecastReflectionUtils.implementsInterface(c, JsonNode.class);
  }
}
