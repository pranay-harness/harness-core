package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for defining ArtifactStream
 */
@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface ArtifactStream extends WithType {}
