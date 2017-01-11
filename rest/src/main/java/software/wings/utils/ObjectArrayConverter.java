package software.wings.utils;

import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

/**
 * Created by peeyushaggarwal on 1/10/17.
 */
public class ObjectArrayConverter extends TypeConverter {
  public ObjectArrayConverter() {
    super(Object[].class);
  }

  @Override
  public Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
    String parameters = fromDBObject.toString();
    return JsonUtils.asObject(parameters, Object[].class, JsonUtils.mapperForCloning);
  }

  @Override
  public Object encode(Object value, MappedField optionalExtraInfo) {
    return JsonUtils.asJson(value, JsonUtils.mapperForCloning);
  }
}
