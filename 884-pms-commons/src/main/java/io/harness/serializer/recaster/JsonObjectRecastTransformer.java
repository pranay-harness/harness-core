package io.harness.serializer.recaster;

import static java.lang.String.format;

import io.harness.beans.CastedField;
import io.harness.exceptions.RecasterException;
import io.harness.pms.yaml.YamlUtils;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

public class JsonObjectRecastTransformer extends RecastTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    try {
      return YamlUtils.read((String) fromObject, targetClass);
    } catch (IOException e) {
      throw new RecasterException(format("Cannot decode JsonObject %s", fromObject));
    }
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    return YamlUtils.write(value);
  }

  @Override
  public boolean isSupported(Class<?> c, CastedField cf) {
    return RecastReflectionUtils.implementsInterface(c, JsonNode.class);
  }
}
