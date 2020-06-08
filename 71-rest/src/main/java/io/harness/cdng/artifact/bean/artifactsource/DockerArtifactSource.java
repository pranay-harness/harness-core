package io.harness.cdng.artifact.bean.artifactsource;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@FieldNameConstants(innerTypeName = "DockerArtifactSourceKeys")
@EqualsAndHashCode(callSuper = true)
public class DockerArtifactSource extends ArtifactSource {
  /** Docker hub registry connector identifier. */
  @NotEmpty String dockerHubConnector;
  /** Images in repos need to be referenced via a path */
  @NotEmpty String imagePath;

  @Builder
  public DockerArtifactSource(String uuid, String accountId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String sourceType, String uniqueHash, String dockerHubConnector,
      String imagePath) {
    super(uuid, accountId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, sourceType, uniqueHash);
    this.dockerHubConnector = dockerHubConnector;
    this.imagePath = imagePath;
  }
}
