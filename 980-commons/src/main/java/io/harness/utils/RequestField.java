/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.exception.UndefinedValueException;

import java.util.Optional;

public class RequestField<T> {
  private static final RequestField<?> ABSENT = new RequestField<>(null, false);
  private static final RequestField<?> EMPTY = new RequestField<>(null, true);

  private final Optional<T> value;
  private final boolean isPresent;

  public static <T> RequestField<T> ofNullable(T value) {
    return new RequestField<>(value, true);
  }

  public static <T> RequestField<T> absent() {
    @SuppressWarnings("unchecked") RequestField<T> i = (RequestField<T>) ABSENT;
    return i;
  }

  public static <T> RequestField<T> ofNull() {
    @SuppressWarnings("unchecked") RequestField<T> i = (RequestField<T>) EMPTY;
    return i;
  }

  private RequestField(T value, boolean isPresent) {
    this.value = Optional.ofNullable(value);
    this.isPresent = isPresent;
  }

  public boolean isPresent() {
    return isPresent;
  }

  public Optional<T> getValue() {
    if (isPresent) {
      return value;
    }
    throw new UndefinedValueException("cannot get value of absent field");
  }

  @Override
  public String toString() {
    return "RequestField{"
        + "value=" + value + ", isPresent=" + isPresent + '}';
  }
}
