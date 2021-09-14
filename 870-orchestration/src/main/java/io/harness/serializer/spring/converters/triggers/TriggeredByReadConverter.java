/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.triggers;

import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.serializer.spring.ProtoReadConverter;

public class TriggeredByReadConverter extends ProtoReadConverter<TriggeredBy> {
  public TriggeredByReadConverter() {
    super(TriggeredBy.class);
  }
}
