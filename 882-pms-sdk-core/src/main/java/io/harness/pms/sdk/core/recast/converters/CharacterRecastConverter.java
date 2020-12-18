package io.harness.pms.sdk.core.recast.converters;

import io.harness.pms.sdk.core.recast.CastedField;
import io.harness.pms.sdk.core.recast.RecastConverter;
import io.harness.pms.sdk.core.recast.RecastReflectionUtils;
import io.harness.pms.sdk.core.recast.RecasterException;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class CharacterRecastConverter extends RecastConverter {
  public CharacterRecastConverter() {
    super(ImmutableList.of(char.class, Character.class));
  }

  @Override
  public Object decode(Class<?> targetClass, Object object, CastedField castedField) {
    if (object == null) {
      return null;
    }

    if (object instanceof String) {
      final char[] chars = ((String) object).toCharArray();
      if (chars.length == 1) {
        return chars[0];
      } else if (chars.length == 0) {
        return (char) 0;
      }
    }
    throw new RecasterException("Trying to map multi-character data to a single character: " + object);
  }

  @Override
  public Object encode(Object object, CastedField castedField) {
    return object;
  }
}
