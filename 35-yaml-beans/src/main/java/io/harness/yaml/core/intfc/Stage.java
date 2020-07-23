package io.harness.yaml.core.intfc;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Base interface for defining Stage
 */
@JsonTypeName("stage")
public interface Stage extends WithIdentifier {}
