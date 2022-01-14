package io.harness.serializer.jackson;

import io.harness.pms.data.OrchestrationMap;
import io.harness.serializer.jackson.json.OrchestrationMapSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class PipelineServiceJacksonModule extends SimpleModule {
  public PipelineServiceJacksonModule() {
    addSerializer(OrchestrationMap.class, new OrchestrationMapSerializer());
  }
}
