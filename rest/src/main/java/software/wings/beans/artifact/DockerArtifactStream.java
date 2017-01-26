package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 1/5/17.
 */
@JsonTypeName("BAMBOO")
public class DockerArtifactStream extends ArtifactStream {
  @NotEmpty private String imageName;

  /**
   * Instantiates a new Docker artifact stream.
   */
  public DockerArtifactStream() {
    super(DOCKER.name());
    super.setAutoApproveForProduction(true);
  }

  @Override
  public String getArtifactDisplayName(String buildNo) {
    return null;
  }

  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageName) {
    this.imageName = imageName;
  }
}
