package io.harness.pms.serializer.jackson;

import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.serializer.json.ExecutionErrorInfoSerializer;
import io.harness.serializer.json.FailureInfoSerializer;
import io.harness.serializer.json.LayoutNodeInfoSerializer;
import io.harness.serializer.json.StepTypeSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class PmsBeansJacksonModule extends SimpleModule {
  public PmsBeansJacksonModule() {
    addSerializer(StepType.class, new StepTypeSerializer());
    addSerializer(FailureInfo.class, new FailureInfoSerializer());
    addSerializer(ExecutionErrorInfo.class, new ExecutionErrorInfoSerializer());
    addSerializer(GraphLayoutInfo.class, new LayoutNodeInfoSerializer());
  }
}
