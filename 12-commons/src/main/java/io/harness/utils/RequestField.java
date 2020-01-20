package io.harness.utils;

import io.harness.exception.UndefinedValueException;

import java.util.Optional;

public class RequestField<T> {
  private static final RequestField<?> NOT_SET = new RequestField<>(null, false);
  private static final RequestField<?> SET_TO_NULL = new RequestField<>(null, true);

  private final Optional<T> value;
  private final boolean hasBeenSet;

  public static <T> RequestField<T> setToNullable(T value) {
    return new RequestField<>(value, true);
  }

  public static <T> RequestField<T> notSet() {
    @SuppressWarnings("unchecked") RequestField<T> i = (RequestField<T>) NOT_SET;
    return i;
  }

  public static <T> RequestField<T> setToNull() {
    @SuppressWarnings("unchecked") RequestField<T> i = (RequestField<T>) SET_TO_NULL;
    return i;
  }

  private RequestField(T value, boolean hasBeenSet) {
    this.value = Optional.ofNullable(value);
    this.hasBeenSet = hasBeenSet;
  }

  public boolean hasBeenSet() {
    return hasBeenSet;
  }

  public Optional<T> getValue() {
    if (hasBeenSet) {
      return value;
    }
    throw new UndefinedValueException("the input value has not been defined");
  }

  @Override
  public String toString() {
    return "RequestField{"
        + "value=" + value + ", hasBeenSet=" + hasBeenSet + '}';
  }
}
