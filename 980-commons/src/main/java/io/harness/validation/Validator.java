/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.validation;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException.ReportTarget;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class Validator {
  public static void notNullCheck(String message, Object value) {
    if (value == null) {
      throw new GeneralException(message);
    }
  }

  public static void notEmptyCheck(String message, String value) {
    if (StringUtils.isEmpty(value)) {
      throw new InvalidRequestException(message);
    }
  }

  public static <T> void notEmptyCheck(String message, Collection<T> value) {
    if (EmptyPredicate.isEmpty(value)) {
      throw new InvalidRequestException(message);
    }
  }

  public static void notBlankCheck(String message, String value) {
    if (StringUtils.isBlank(value)) {
      throw new InvalidRequestException(message);
    }
  }

  public static void notNullCheck(String message, Object value, EnumSet<ReportTarget> reportTargets) {
    if (value == null) {
      throw new GeneralException(message, reportTargets);
    }
  }

  public static void nullCheckForInvalidRequest(
      Object value, @NotNull String message, @NotNull EnumSet<ReportTarget> reportTargets) {
    if (value == null) {
      throw new InvalidRequestException(message, reportTargets);
    }
  }

  public static void nullCheck(String message, Object value) {
    if (value != null) {
      throw new GeneralException(message);
    }
  }

  public static void equalCheck(Object value1, Object value2) {
    if (!Objects.equals(value1, value2)) {
      throw new InvalidRequestException("Not equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void unEqualCheck(Object value1, Object value2) {
    if (Objects.equals(value1, value2)) {
      throw new InvalidRequestException("Equal -  value1: " + value1 + ", value2: " + value2);
    }
  }

  public static void ensureType(Class clazz, Object object, String errorMsg) {
    if (!(clazz.isInstance(object))) {
      throw new InvalidRequestException(errorMsg);
    }
  }
}
