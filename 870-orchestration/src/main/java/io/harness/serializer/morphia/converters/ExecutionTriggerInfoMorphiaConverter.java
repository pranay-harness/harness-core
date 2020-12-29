package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.ambiance.ExecutionTriggerInfo;

public class ExecutionTriggerInfoMorphiaConverter extends ProtoMessageConverter<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoMorphiaConverter() {
    super(ExecutionTriggerInfo.class);
  }
}
