package io.harness.cdng.artifact.bean;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@FieldNameConstants(innerTypeName = "DockerArtifactKeys")
@EqualsAndHashCode(callSuper = true)
public class DockerArtifact extends Artifact {
  /** Docker hub registry connector uuid. */
  @NotEmpty String dockerHubConnectorId;

  /** Images in repos need to be referenced via a path */
  @NotEmpty String imagePath;

  /** Tag refers to exact tag number */
  @NotEmpty String tag;

  @Builder
  public DockerArtifact(String uuid, String accountId, String sourceType, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String artifactSourceId, String dockerHubConnectorId,
      String imagePath, String tag) {
    super(uuid, accountId, sourceType, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, artifactSourceId);
    this.dockerHubConnectorId = dockerHubConnectorId;
    this.imagePath = imagePath;
    this.tag = tag;
  }
}
