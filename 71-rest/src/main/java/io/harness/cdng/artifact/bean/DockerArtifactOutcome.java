package io.harness.cdng.artifact.bean;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
public class DockerArtifactOutcome implements ArtifactOutcome {
  /** Docker hub registry connector. */
  String dockerhubConnector;
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
}
