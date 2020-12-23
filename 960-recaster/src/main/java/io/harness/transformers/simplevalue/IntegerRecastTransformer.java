package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class IntegerRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public IntegerRecastTransformer() {
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
