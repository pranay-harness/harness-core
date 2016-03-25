package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.ArtifactSource.ArtifactType;

import java.util.ArrayList;
import java.util.List;

/**
 *  Component bean class.
 *
 *
 * @author Rishi
 *
 */

@Entity(value = "services", noClassnameStored = true)
public class Service extends Base {
  private String name;
  private String description;
  private ArtifactType artifactType;
  q @Reference(idOnly = true, ignoreMissing = true) private List<PlatformSoftware> platformSoftwares;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ArtifactType getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public List<PlatformSoftware> getPlatformSoftwares() {
    return platformSoftwares;
  }

  public void setPlatformSoftwares(List<PlatformSoftware> platformSoftwares) {
    this.platformSoftwares = platformSoftwares;
  }

  public void addPlatformSoftware(PlatformSoftware platformSoftware) {
    if (platformSoftwares == null) {
      platformSoftwares = new ArrayList<>();
    }
    platformSoftwares.add(platformSoftware);
  }
}
