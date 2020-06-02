package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.core.defaults.DefaultStageProperties;
import io.harness.yaml.core.deserializer.StagePolymorphicDeserializer;

/**
 * Base interface for defining Stage
 */
@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
@JsonTypeName("stage")
@JsonDeserialize(using = StagePolymorphicDeserializer.class)
public interface Stage extends StageWrapper, WithType, WithIdentifier, DefaultStageProperties {}
