package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NONE;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;
import io.harness.yaml.core.deserializer.StepPolymorphicDeserializer;

/**
 * Base interface for defining different Step types
 */
@JsonTypeInfo(use = NONE)
@JsonDeserialize(using = StepPolymorphicDeserializer.class)
@JsonTypeName("step")
public interface StepInfo extends ExecutionSection, StepWrapper, WithIdentifier {}
