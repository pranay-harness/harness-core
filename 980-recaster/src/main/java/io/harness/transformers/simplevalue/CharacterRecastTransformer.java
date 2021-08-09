package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.transformers.RecastTransformer;

import com.google.common.collect.ImmutableList;

public class CharacterRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  public CharacterRecastTransformer() {
    super(ImmutableList.of(char.class, Character.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    if (object == null) {
      return null;
    }

    if (object instanceof Character) {
      return object;
    }

    return String.valueOf(object).toCharArray()[0];
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object == null || object.equals('\0') ? null : String.valueOf(object).toCharArray()[0];
  }
}
