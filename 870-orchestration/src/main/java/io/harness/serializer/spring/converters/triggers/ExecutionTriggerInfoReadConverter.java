/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.triggers;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class ExecutionTriggerInfoReadConverter extends ProtoReadConverter<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoReadConverter() {
    super(ExecutionTriggerInfo.class);
  }
}
