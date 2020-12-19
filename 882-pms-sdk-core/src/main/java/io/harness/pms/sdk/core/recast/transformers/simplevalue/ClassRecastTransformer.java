package io.harness.pms.sdk.core.recast.transformers.simplevalue;

import io.harness.pms.sdk.core.recast.RecastTransformer;
import io.harness.pms.sdk.core.recast.beans.CastedField;

import java.util.Collections;
import org.mongodb.morphia.mapping.MappingException;

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
      throw new MappingException("Cannot create class from Name '" + l + "'", e);
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
