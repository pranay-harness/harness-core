package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStream.ArtifactStreamType.DOCKER;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("BAMBOO")
public class DockerArtifactStream extends ArtifactStream {
  @NotEmpty private String imageName;
  private String command;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public DockerArtifactStream() {
    super(DOCKER);
  }

  @Override
  public String getArtifactDisplayName(int buildNo) {
    return null;
  }
}
