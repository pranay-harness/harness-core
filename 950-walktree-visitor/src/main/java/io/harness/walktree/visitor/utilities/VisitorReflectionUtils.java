package io.harness.walktree.visitor.utilities;

import java.lang.reflect.Field;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VisitorReflectionUtils {
  public Field addValueToField(Object element, Field field, Object value) throws IllegalAccessException {
    field.setAccessible(true);
    field.set(element, value);
    return field;
  }
}
