package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;
import io.harness.pms.sdk.core.recast.utils.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class DoubleRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public DoubleRecastTransformer() {
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
