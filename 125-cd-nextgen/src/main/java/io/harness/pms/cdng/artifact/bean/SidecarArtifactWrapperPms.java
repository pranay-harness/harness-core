package io.harness.pms.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.beans.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * wrapper object for sidecar element.
 * sidecars:
 *      - sidecar:
 *              identifier:
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface SidecarArtifactWrapperPms extends WithIdentifier {
  @JsonIgnore ArtifactConfigPms getArtifactConfigPms();
}
