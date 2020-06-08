package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;

/**
 * wrapper object for dockerhub, gcr, etc element.
 * artifacts:
 *      primary:
 *             dockerhub:
 *      sidecars
 *          -sidecar:
 *              identifier:
 *              dockerhub:
 */
@JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT)
public interface ArtifactConfigWrapper {
  String getSourceType();
  String getUniqueHash();
  ArtifactSource getArtifactSource(String accountId);
  ArtifactSourceAttributes getSourceAttributes();
}
