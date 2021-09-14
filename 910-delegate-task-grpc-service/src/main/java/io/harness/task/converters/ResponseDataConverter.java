/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.task.converters;

import io.harness.tasks.ResponseData;

public interface ResponseDataConverter<T> {
  T convert(ResponseData responseData);
  ResponseData convert(T t);
}
