/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.triggers.TriggerPayload;

public class TriggerPayloadMorphiaConverter extends ProtoMessageConverter<TriggerPayload> {
  public TriggerPayloadMorphiaConverter() {
    super(TriggerPayload.class);
  }
}
