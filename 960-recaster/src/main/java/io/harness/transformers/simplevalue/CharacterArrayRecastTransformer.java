package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.Array;

public class CharacterArrayRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public CharacterArrayRecastTransformer() {
    super(ImmutableList.of(char[].class, Character[].class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    return object;
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object;
  }

  Object convertToWrapperArray(final Character[] values) {
    final int length = values.length;
    final Object array = Array.newInstance(Character.class, length);
    for (int i = 0; i < length; i++) {
      Array.set(array, i, values[i]);
    }
    return array;
  }
}
