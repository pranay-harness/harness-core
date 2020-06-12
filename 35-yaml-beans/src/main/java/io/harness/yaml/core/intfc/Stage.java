package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NONE;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.core.deserializer.StagePolymorphicDeserializer;

/**
 * Base interface for defining Stage
 */
@JsonTypeInfo(use = NONE)
@JsonTypeName("stage")
@JsonDeserialize(using = StagePolymorphicDeserializer.class)
public interface Stage extends StageWrapper, WithIdentifier {}
