package io.harness.delegate.task.stepstatus;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    visible = true, defaultImpl = StepMapOutput.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = StepMapOutput.class, name = "MAP") })
public interface StepOutput {
  enum Type { MAP }
  StepOutput.Type getType();
}
