package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

public class EnumRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  @Override
  @SuppressWarnings("unchecked")
  public Object decode(final Class targetClass, final Object fromDBObject, final CastedField castedField) {
    if (fromDBObject == null) {
      return null;
    }
    return Enum.valueOf(targetClass, fromDBObject.toString());
  }

  @Override
  public Object encode(final Object value, final CastedField castedField) {
    if (value == null) {
      return null;
    }

    return getName((Enum) value);
  }

  @Override
  protected boolean isSupported(final Class c, final CastedField castedField) {
    return c.isEnum();
  }

  private <T extends Enum> String getName(final T value) {
    return value.name();
  }
}
