package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Base interface for defining Pipeline workflow. Each pipeline needs to have unique identifier.
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
@JsonTypeName("pipeline")
public interface Pipeline extends WithIdentifier {}
