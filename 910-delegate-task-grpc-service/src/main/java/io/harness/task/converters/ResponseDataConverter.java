package io.harness.task.converters;

import io.harness.tasks.ResponseData;

public interface ResponseDataConverter<T> {
  T convert(ResponseData responseData);
  ResponseData convert(T t);
}
