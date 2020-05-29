package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.PreviousStageAware;

/**
 * Base interface for defining Infrastructure
 */
@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface Infrastructure extends WithType, PreviousStageAware {}
