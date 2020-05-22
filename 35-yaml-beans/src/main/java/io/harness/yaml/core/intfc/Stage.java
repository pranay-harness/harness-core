package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.defaults.DefaultStageProperties;

/**
 * Base interface for defining Stage
 */
@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface Stage extends WithType, WithIdentifier, DefaultStageProperties {}
