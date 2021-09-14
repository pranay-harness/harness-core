/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.timeout.obtainment;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.spring.ProtoReadConverter;
import io.harness.timeout.contracts.TimeoutObtainment;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class TimeoutObtainmentReadConverter extends ProtoReadConverter<TimeoutObtainment> {
  public TimeoutObtainmentReadConverter() {
    super(TimeoutObtainment.class);
  }
}
