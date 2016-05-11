package software.wings.beans;

import static java.util.stream.Collectors.toSet;
import static software.wings.beans.Service.ServiceBuilder.aService;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.utils.validation.FutureDate;

import java.util.List;
import java.util.Set;

/**
 * Release bean class.
 *
 * @author Rishi
 */
@Entity(value = "releases")
public class Release extends Base {
  @NotEmpty private String releaseName;
  @NotEmpty private String description;
  @FutureDate private long targetDate;

  private List<ArtifactSource> artifactSources = Lists.newArrayList();

  private Status status = Status.ACTIVE;

  public String getReleaseName() {
    return releaseName;
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

  public long getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(long targetDate) {
    this.targetDate = targetDate;
  }

  public List<ArtifactSource> getArtifactSources() {
    return artifactSources;
  }

  public void setArtifactSources(List<ArtifactSource> artifactSources) {
    this.artifactSources = artifactSources;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @JsonProperty("services")
  public Set<Service> getServices() {
    return artifactSources.stream()
        .flatMap(artifactSource -> artifactSource.getServices().stream())
        .map(service -> aService().withUuid(service.getUuid()).withName(service.getName()).build())
        .collect(toSet());
  }

  public ArtifactSource get(String artifactSourceName) {
    return getArtifactSources()
        .stream()
        .filter(artifactSource -> artifactSource.getSourceName().equals(artifactSourceName))
        .findFirst()
        .orElse(null);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    Release release = (Release) obj;
    return targetDate == release.targetDate && Objects.equal(releaseName, release.releaseName)
        && Objects.equal(description, release.description) && Objects.equal(artifactSources, release.artifactSources)
        && status == release.status;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), releaseName, description, targetDate, artifactSources, status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("releaseName", releaseName)
        .add("description", description)
        .add("targetDate", targetDate)
        .add("artifactSources", artifactSources)
        .add("status", status)
        .toString();
  }

  public enum Status {
    ACTIVE,
    INACTIVE,
    FINALIZED;
  }

  public static final class ReleaseBuilder {
    private String releaseName;
    private String description;
    private long targetDate;
    private List<ArtifactSource> artifactSources = Lists.newArrayList();
    private Status status = Status.ACTIVE;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ReleaseBuilder() {}

    public static ReleaseBuilder aRelease() {
      return new ReleaseBuilder();
    }

    public ReleaseBuilder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public ReleaseBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ReleaseBuilder withTargetDate(long targetDate) {
      this.targetDate = targetDate;
      return this;
    }

    public ReleaseBuilder withArtifactSources(List<ArtifactSource> artifactSources) {
      this.artifactSources = artifactSources;
      return this;
    }

    public ReleaseBuilder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public ReleaseBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ReleaseBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ReleaseBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ReleaseBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ReleaseBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ReleaseBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ReleaseBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ReleaseBuilder but() {
      return aRelease()
          .withReleaseName(releaseName)
          .withDescription(description)
          .withTargetDate(targetDate)
          .withArtifactSources(artifactSources)
          .withStatus(status)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Release build() {
      Release release = new Release();
      release.setReleaseName(releaseName);
      release.setDescription(description);
      release.setTargetDate(targetDate);
      release.setArtifactSources(artifactSources);
      release.setStatus(status);
      release.setUuid(uuid);
      release.setAppId(appId);
      release.setCreatedBy(createdBy);
      release.setCreatedAt(createdAt);
      release.setLastUpdatedBy(lastUpdatedBy);
      release.setLastUpdatedAt(lastUpdatedAt);
      release.setActive(active);
      return release;
    }
  }
}
