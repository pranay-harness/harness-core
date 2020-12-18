package io.harness.pms.sdk.core.recast.converters;

import io.harness.pms.sdk.core.recast.CastedField;
import io.harness.pms.sdk.core.recast.RecastConverter;

import java.util.Collections;
import org.mongodb.morphia.mapping.MappingException;

public class ClassRecastConverter extends RecastConverter {
  /**
   * Creates the Converter.
   */
  public ClassRecastConverter() {
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
