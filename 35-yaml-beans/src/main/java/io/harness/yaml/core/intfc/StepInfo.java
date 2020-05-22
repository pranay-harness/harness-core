package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StepWrapper;
import io.harness.yaml.core.defaults.DefaultStepProperties;
import io.harness.yaml.core.deserializer.StepPolymorphicDeserializer;

/**
 * Base interface for defining different Step types
 */
@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
@JsonDeserialize(using = StepPolymorphicDeserializer.class)
public interface StepInfo extends ExecutionSection, StepWrapper, WithType, WithIdentifier, DefaultStepProperties {}
