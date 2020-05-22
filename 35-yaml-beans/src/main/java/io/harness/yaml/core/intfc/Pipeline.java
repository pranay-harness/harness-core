package io.harness.yaml.core.intfc;

import io.harness.yaml.core.defaults.DefaultPipelineProperties;

/**
 * Base interface for defining Pipeline workflow. Each pipeline needs to have unique identifier.
 */
public interface Pipeline extends WithIdentifier, DefaultPipelineProperties {}
