package io.harness.pms.sdk.core.recast.converters;

import io.harness.pms.sdk.core.recast.CastedField;
import io.harness.pms.sdk.core.recast.RecastConverter;
import io.harness.pms.sdk.core.recast.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class IntegerRecastConverter extends RecastConverter {
  public IntegerRecastConverter() {
    super(ImmutableList.of(int.class, Integer.class, int[].class, Integer[].class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    if (object == null) {
      return null;
    }

    if (object instanceof Integer) {
      return object;
    }

    if (object instanceof Number) {
      return ((Number) object).intValue();
    }

    if (object instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return RecastReflectionUtils.convertToArray(type, (List<?>) object);
    }

    return Integer.parseInt(object.toString());
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object;
  }
}
