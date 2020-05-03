package io.harness.persistence.converters;

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import java.time.Duration;

public class DurationConverter extends TypeConverter implements SimpleValueConverter {
  public DurationConverter() {
    super(Duration.class);
  }

  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    if (value == null) {
      return null;
    }
    return ((Duration) value).toMillis();
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    if (fromDBObject == null) {
      return null;
    }
    return Duration.ofMillis((long) fromDBObject);
  }
}
