package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.yaml.core.intfc.WithIdentifier;

/**
 * wrapper object for sidecar element.
 * sidecars:
 *      - sidecar:
 *              identifier:
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface SidecarArtifactWrapper extends WithIdentifier {
  ArtifactConfigWrapper getArtifact();
}
