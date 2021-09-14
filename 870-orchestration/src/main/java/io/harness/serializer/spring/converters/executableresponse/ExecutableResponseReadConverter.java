/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.executableresponse;

import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@Singleton
@ReadingConverter
public class ExecutableResponseReadConverter extends ProtoReadConverter<ExecutableResponse> {
  public ExecutableResponseReadConverter() {
    super(ExecutableResponse.class);
  }
}
