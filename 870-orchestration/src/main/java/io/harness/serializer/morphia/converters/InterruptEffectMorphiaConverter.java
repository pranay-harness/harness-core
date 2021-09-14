/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia.converters;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;

@OwnedBy(PIPELINE)
public class InterruptEffectMorphiaConverter extends ProtoMessageConverter<InterruptEffectProto> {
  public InterruptEffectMorphiaConverter() {
    super(InterruptEffectProto.class);
  }
}
