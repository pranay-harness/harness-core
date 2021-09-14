/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.outputs;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.output.PmsSweepingOutput;
import io.harness.serializer.KryoSerializer;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapAbstractWriteConverter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class PmsSweepingOutputWriteConverter extends OrchestrationMapAbstractWriteConverter<PmsSweepingOutput> {
  @Inject
  public PmsSweepingOutputWriteConverter(KryoSerializer kryoSerializer) {
    super(kryoSerializer);
  }
}
