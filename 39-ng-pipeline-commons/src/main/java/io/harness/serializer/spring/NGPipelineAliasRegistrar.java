package io.harness.serializer.spring;

import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

public class NGPipelineAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("io.harness.ngpipeline.pipeline.beans.yaml.ngPipeline", NgPipeline.class);
    orchestrationElements.put("io.harness.ngpipeline.pipeline.beans.entities.ngPipelineEntity", NgPipelineEntity.class);
    orchestrationElements.put(
        "io.harness.ngpipeline.status.BuildStatusUpdateParameter", BuildStatusUpdateParameter.class);
  }
}
