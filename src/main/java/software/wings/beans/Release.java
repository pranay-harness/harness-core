package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.utils.validation.FutureDate;

import java.util.List;
import java.util.Set;

// TODO: Auto-generated Javadoc

/**
 * Release bean class.
 *
 * @author Rishi
 */
@Entity(value = "releases")
public class Release extends Base {
  @NotEmpty private String releaseName;
  private String description;
  @FutureDate private Long targetDate;

  @Reference(idOnly = true) private Set<Service> services;

  private List<ArtifactSource> artifactSources = Lists.newArrayList();

  private Status status = Status.ACTIVE;

  /**
   * Gets release name.
   *
   * @return the release name
   */
  public String getReleaseName() {
    return releaseName;
  }

  /**
   * Sets release name.
   *
   * @param releaseName the release name
   */
  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets target date.
   *
   * @return the target date
   */
  public Long getTargetDate() {
    return targetDate;
  }

  /**
   * Sets target date.
   *
   * @param targetDate the target date
   */
  public void setTargetDate(Long targetDate) {
    this.targetDate = targetDate;
  }

  /**
   * Gets artifact sources.
   *
   * @return the artifact sources
   */
  public List<ArtifactSource> getArtifactSources() {
    return artifactSources;
  }

  /**
   * Sets artifact sources.
   *
   * @param artifactSources the artifact sources
   */
  public void setArtifactSources(List<ArtifactSource> artifactSources) {
    this.artifactSources = artifactSources;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public Set<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(Set<Service> services) {
    this.services = services;
  }

  /**
   * Gets the.
   *
   * @param artifactSourceName the artifact source name
   * @return the artifact source
   */
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
    return Objects.equal(releaseName, release.releaseName) && Objects.equal(description, release.description)
        && Objects.equal(targetDate, release.targetDate) && Objects.equal(artifactSources, release.artifactSources)
        && status == release.status;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), releaseName, description, targetDate, artifactSources, status);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
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

  /**
   * The Enum Status.
   */
  public enum Status {
    /**
     * Active status.
     */
    ACTIVE, /**
             * Inactive status.
             */
    INACTIVE, /**
               * Finalized status.
               */
    FINALIZED;
  }

  /**
   * The Class ReleaseBuilder.
   */
  public static final class ReleaseBuilder {
    private String releaseName;
    private String description;
    private Long targetDate;
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

    /**
     * A release.
     *
     * @return the release builder
     */
    public static ReleaseBuilder aRelease() {
      return new ReleaseBuilder();
    }

    /**
     * With release name.
     *
     * @param releaseName the release name
     * @return the release builder
     */
    public ReleaseBuilder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    /**
     * With description.
     *
     * @param description the description
     * @return the release builder
     */
    public ReleaseBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With target date.
     *
     * @param targetDate the target date
     * @return the release builder
     */
    public ReleaseBuilder withTargetDate(Long targetDate) {
      this.targetDate = targetDate;
      return this;
    }

    /**
     * With artifact sources.
     *
     * @param artifactSources the artifact sources
     * @return the release builder
     */
    public ReleaseBuilder withArtifactSources(List<ArtifactSource> artifactSources) {
      this.artifactSources = artifactSources;
      return this;
    }

    /**
     * With status.
     *
     * @param status the status
     * @return the release builder
     */
    public ReleaseBuilder withStatus(Status status) {
      this.status = status;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the release builder
     */
    public ReleaseBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the release builder
     */
    public ReleaseBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the release builder
     */
    public ReleaseBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the release builder
     */
    public ReleaseBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the release builder
     */
    public ReleaseBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the release builder
     */
    public ReleaseBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the release builder
     */
    public ReleaseBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the release builder
     */
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

    /**
     * Builds the.
     *
     * @return the release
     */
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
