package io.harness.delegate.task.stepstatus;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StepMapOutput.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = StepMapOutput.class, name = "stepMapOutput") })
public interface StepOutput {}
