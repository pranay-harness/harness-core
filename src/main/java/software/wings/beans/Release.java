package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Release bean class.
 *
 * @author Rishi
 */
@Entity(value = "releases", noClassnameStored = true)
public class Release extends Base {
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

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
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

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), application, releaseName, description, artifactSources, svcArtifactSourceMap,
        svcPlatformMap, status);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Release release = (Release) o;
    return Objects.equals(application, release.application) && Objects.equals(releaseName, release.releaseName)
        && Objects.equals(description, release.description) && Objects.equals(artifactSources, release.artifactSources)
        && Objects.equals(svcArtifactSourceMap, release.svcArtifactSourceMap)
        && Objects.equals(svcPlatformMap, release.svcPlatformMap) && status == release.status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("application", application)
        .add("releaseName", releaseName)
        .add("description", description)
        .add("artifactSources", artifactSources)
        .add("svcArtifactSourceMap", svcArtifactSourceMap)
        .add("svcPlatformMap", svcPlatformMap)
        .add("status", status)
        .toString();
  }

  public enum Status {
    ACTIVE,
    INACTIVE,
    FINALIZED;
  }

  public static class Builder {
    private Application application;
    private String releaseName;
    private String description;
    private Map<String, ArtifactSource> artifactSources = new HashMap<>();
    private Map<String, String> svcArtifactSourceMap = new HashMap<>();
    private Map<String, String> svcPlatformMap = new HashMap<>();
    private Status status = Status.ACTIVE;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public Builder but() {
      return aRelease()
          .withApplication(application)
          .withReleaseName(releaseName)
          .withDescription(description)
          .withArtifactSources(artifactSources)
          .withSvcArtifactSourceMap(svcArtifactSourceMap)
          .withSvcPlatformMap(svcPlatformMap)
          .withStatus(status)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withSvcPlatformMap(Map<String, String> svcPlatformMap) {
      this.svcPlatformMap = svcPlatformMap;
      return this;
    }

    public Builder withSvcArtifactSourceMap(Map<String, String> svcArtifactSourceMap) {
      this.svcArtifactSourceMap = svcArtifactSourceMap;
      return this;
    }

    public Builder withArtifactSources(Map<String, ArtifactSource> artifactSources) {
      this.artifactSources = artifactSources;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public Builder withApplication(Application application) {
      this.application = application;
      return this;
    }

    public static Builder aRelease() {
      return new Builder();
    }

    public Release build() {
      Release release = new Release();
      release.setApplication(application);
      release.setReleaseName(releaseName);
      release.setDescription(description);
      release.setArtifactSources(artifactSources);
      release.setSvcArtifactSourceMap(svcArtifactSourceMap);
      release.setSvcPlatformMap(svcPlatformMap);
      release.setStatus(status);
      release.setUuid(uuid);
      release.setCreatedBy(createdBy);
      release.setCreatedAt(createdAt);
      release.setLastUpdatedBy(lastUpdatedBy);
      release.setLastUpdatedAt(lastUpdatedAt);
      release.setActive(active);
      return release;
    }
  }
}
