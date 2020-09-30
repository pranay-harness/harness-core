package io.harness.serializer.spring;

import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class NGPipelineAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("io.harness.cdng.pipeline.beans.entities.pipelinesNG", NgPipelineEntity.class);
  }
}
