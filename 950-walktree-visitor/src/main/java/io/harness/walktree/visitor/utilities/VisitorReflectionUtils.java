/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
