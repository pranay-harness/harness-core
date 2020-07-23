package io.harness.yaml.core.intfc;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Base interface for defining different Step types
 */
@JsonTypeName("step")
public interface StepInfo extends WithIdentifier {}
