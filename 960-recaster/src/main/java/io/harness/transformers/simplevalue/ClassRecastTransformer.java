package io.harness.transformers.simplevalue;

import io.harness.beans.CastedField;
import io.harness.exceptions.RecasterException;
import io.harness.transformers.RecastTransformer;

import java.util.Collections;

public class ClassRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  /**
   * Creates the Converter.
   */
  public ClassRecastTransformer() {
    super(Collections.singletonList(Class.class));
  }

  @Override
  public Object decode(final Class targetClass, final Object fromObject, final CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    final String l = fromObject.toString();
    try {
      return Class.forName(l);
    } catch (ClassNotFoundException e) {
      throw new RecasterException("Cannot create class from Name '" + l + "'", e);
    }
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    if (value == null) {
      return null;
    } else {
      return ((Class) value).getName();
    }
  }
}
