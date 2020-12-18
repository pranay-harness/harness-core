package io.harness.pms.sdk.core.recast.converters;

import io.harness.pms.sdk.core.recast.CastedField;
import io.harness.pms.sdk.core.recast.RecastConverter;
import io.harness.pms.sdk.core.recast.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class DoubleRecastConverter extends RecastConverter {
  public DoubleRecastConverter() {
    super(ImmutableList.of(double.class, Double.class, double[].class, Double[].class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    if (object == null) {
      return null;
    }

    if (object instanceof Double) {
      return object;
    }

    if (object instanceof Number) {
      return ((Number) object).doubleValue();
    }

    if (object instanceof List) {
      final Class<?> type = targetClass.isArray() ? targetClass.getComponentType() : targetClass;
      return RecastReflectionUtils.convertToArray(type, (List<?>) object);
    }

    return Double.parseDouble(object.toString());
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object;
  }
}
