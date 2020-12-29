package io.harness.serializer.spring.converters.triggers;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class ExecutionTriggerInfoReadConverter extends ProtoReadConverter<ExecutionTriggerInfo> {
  public ExecutionTriggerInfoReadConverter() {
    super(ExecutionTriggerInfo.class);
  }
}
