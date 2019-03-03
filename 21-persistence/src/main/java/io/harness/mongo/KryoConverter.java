package io.harness.mongo;

import io.harness.serializer.KryoUtils;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

public class KryoConverter extends TypeConverter {
  protected KryoConverter(final Class... types) {
    super(types);
  }

  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }
    return KryoUtils.asBytes(value);
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    if (fromDBObject == null) {
      return null;
    }
    return KryoUtils.asObject((byte[]) fromDBObject);
  }
}
