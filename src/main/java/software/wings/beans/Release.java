package software.wings.beans;

import java.util.HashMap;
import java.util.Map;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

/**
 *  Release bean class.
 *
 *
 * @author Rishi
 *
 */
@Entity(value = "releases", noClassnameStored = true)
public class Release extends Base {
  public enum Status {
    ACTIVE,
    INACTIVE,
    FINALIZED;
  }

  @Indexed @Reference(idOnly = true) private Application application;

  private String releaseName;
  private String description;

  private Map<String, ArtifactSource> artifactSources = new HashMap<>();

  private Map<String, String> svcArtifactSourceMap = new HashMap<>();

  private Map<String, String> svcPlatformMap = new HashMap<>();

  private Status status = Status.ACTIVE;

  public String getReleaseName() {
    return releaseName;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, ArtifactSource> getArtifactSources() {
    return artifactSources;
  }

  public void setArtifactSources(Map<String, ArtifactSource> artifactSources) {
    this.artifactSources = artifactSources;
  }

  public Map<String, String> getSvcArtifactSourceMap() {
    return svcArtifactSourceMap;
  }

  public void setSvcArtifactSourceMap(Map<String, String> svcArtifactSourceMap) {
    this.svcArtifactSourceMap = svcArtifactSourceMap;
  }

  public Map<String, String> getSvcPlatformMap() {
    return svcPlatformMap;
  }

  public void setSvcPlatformMap(Map<String, String> svcPlatformMap) {
    this.svcPlatformMap = svcPlatformMap;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void addArtifactSources(String svcName, ArtifactSource artifactSource) {
    artifactSources.put(artifactSource.getSourceName(), artifactSource);
    svcArtifactSourceMap.put(svcName, artifactSource.getSourceName());
  }
}
