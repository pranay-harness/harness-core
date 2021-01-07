package io.harness.pms.serializer.jackson;

import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.GraphLayoutInfo;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.contracts.steps.StepType;
import io.harness.serializer.json.ExecutableResponseSerializer;
import io.harness.serializer.json.ExecutionErrorInfoSerializer;
import io.harness.serializer.json.ExecutionMetadataSerializer;
import io.harness.serializer.json.ExecutionTriggerInfoSerializer;
import io.harness.serializer.json.FailureInfoSerializer;
import io.harness.serializer.json.LayoutNodeInfoSerializer;
import io.harness.serializer.json.StepTypeSerializer;
import io.harness.serializer.json.TriggeredBySerializer;
import io.harness.serializer.json.YamlPropertiesSerializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class PmsBeansJacksonModule extends SimpleModule {
  public PmsBeansJacksonModule() {
    addSerializer(StepType.class, new StepTypeSerializer());
    addSerializer(FailureInfo.class, new FailureInfoSerializer());
    addSerializer(ExecutionErrorInfo.class, new ExecutionErrorInfoSerializer());
    addSerializer(GraphLayoutInfo.class, new LayoutNodeInfoSerializer());
    addSerializer(ExecutionTriggerInfo.class, new ExecutionTriggerInfoSerializer());
    addSerializer(TriggeredBy.class, new TriggeredBySerializer());
    addSerializer(ExecutionMetadata.class, new ExecutionMetadataSerializer());
    addSerializer(YamlProperties.class, new YamlPropertiesSerializer());
    addSerializer(ExecutableResponse.class, new ExecutableResponseSerializer());
  }
}
