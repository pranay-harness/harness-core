package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.DelegateArtifactService;
import io.harness.cdng.artifact.delegate.DockerArtifactServiceImpl;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * DTO object to be passed to delegate tasks.
 */
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactSourceAttributes implements ArtifactSourceAttributes {
  /** Docker hub registry connector. */
  String dockerhubConnector;
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;

  @Override
  public Class<? extends DelegateArtifactService> getDelegateArtifactServiceClass() {
    return DockerArtifactServiceImpl.class;
  }
}
