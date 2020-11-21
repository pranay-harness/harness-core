package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.yaml.core.intfc.OverridesApplier;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * wrapper object for dockerhub, gcr, etc element.
 * artifacts:
 *      primary:
 *             type: dockerhub
 *             spec:
 *      sidecars
 *          -sidecar:
 *              identifier:
 *              type: dockerhub
 *              spec:
 */
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface ArtifactConfig extends WithIdentifier, OverridesApplier<ArtifactConfig> {
  @JsonIgnore ArtifactSourceType getSourceType();
  @JsonIgnore String getUniqueHash();
  @JsonIgnore boolean isPrimaryArtifact();
  @JsonIgnore void setPrimaryArtifact(boolean primaryArtifact);
  void setIdentifier(String identifier);
  @Override @JsonIgnore String getIdentifier();
}
