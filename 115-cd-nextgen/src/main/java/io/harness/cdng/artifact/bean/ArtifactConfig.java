package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.delegate.beans.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.ArtifactSourceAttributes;
import io.harness.yaml.core.intfc.OverridesApplier;
import io.harness.yaml.core.intfc.WithIdentifier;

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
  @JsonIgnore String getSourceType();
  @JsonIgnore String getUniqueHash();
  @JsonIgnore ArtifactSource getArtifactSource(String accountId);
  @JsonIgnore ArtifactSourceAttributes getSourceAttributes();
  @JsonIgnore String getArtifactType();
  @JsonIgnore void setArtifactType(String artifactType);
  void setIdentifier(String identifier);
  @Override @JsonIgnore String getIdentifier();
  @JsonIgnore ArtifactOutcome getArtifactOutcome(ArtifactAttributes artifactAttributes);
}
