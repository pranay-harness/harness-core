/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.jackson;

import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.jackson.json.OrchestrationMapSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class PipelineServiceJacksonModule extends SimpleModule {
  public PipelineServiceJacksonModule() {
    addSerializer(OrchestrationMap.class, new OrchestrationMapSerializer());
  }
}
