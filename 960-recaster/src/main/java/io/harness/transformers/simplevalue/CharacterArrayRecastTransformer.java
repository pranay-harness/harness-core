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
    if (object == null) {
      return null;
    }

    final char[] chars = object.toString().toCharArray();
    if (targetClass.isArray() && targetClass.equals(Character[].class)) {
      return convertToWrapperArray(chars);
    }
    return chars;
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    if (object == null) {
      return null;
    } else {
      if (object instanceof char[]) {
        return new String((char[]) object);
      } else {
        final StringBuilder builder = new StringBuilder();
        final Character[] array = (Character[]) object;
        for (final Character character : array) {
          builder.append(character);
        }
        return builder.toString();
      }
    }
  }

  Object convertToWrapperArray(final char[] values) {
    final int length = values.length;
    final Object array = Array.newInstance(Character.class, length);
    for (int i = 0; i < length; i++) {
      Array.set(array, i, values[i]);
    }
    return array;
  }
}
