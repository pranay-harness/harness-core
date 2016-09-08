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

  @Reference(idOnly = true, ignoreMissing = true) private Set<Service> services;

  private List<ArtifactSource> artifactSources = Lists.newArrayList();

  private List<BreakdownByEnvironments> breakdownByEnvironments;

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
   * Gets breakdown by environments.
   *
   * @return the breakdown by environments
   */
  public List<BreakdownByEnvironments> getBreakdownByEnvironments() {
    return breakdownByEnvironments;
  }

  /**
   * Sets breakdown by environments.
   *
   * @param breakdownByEnvironments the breakdown by environments
   */
  public void setBreakdownByEnvironments(List<BreakdownByEnvironments> breakdownByEnvironments) {
    this.breakdownByEnvironments = breakdownByEnvironments;
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
   * Created by peeyushaggarwal on 7/13/16.
   */
  public static class BreakdownByEnvironments {
    private String envId;
    private String envName;
    private int total;
    private CountsByStatuses breakdown;

    /**
     * Getter for property 'envId'.
     *
     * @return Value for property 'envId'.
     */
    public String getEnvId() {
      return envId;
    }

    /**
     * Setter for property 'envId'.
     *
     * @param envId Value to set for property 'envId'.
     */
    public void setEnvId(String envId) {
      this.envId = envId;
    }

    /**
     * Getter for property 'envName'.
     *
     * @return Value for property 'envName'.
     */
    public String getEnvName() {
      return envName;
    }

    /**
     * Setter for property 'envName'.
     *
     * @param envName Value to set for property 'envName'.
     */
    public void setEnvName(String envName) {
      this.envName = envName;
    }

    /**
     * Getter for property 'total'.
     *
     * @return Value for property 'total'.
     */
    public int getTotal() {
      return total;
    }

    /**
     * Setter for property 'total'.
     *
     * @param total Value to set for property 'total'.
     */
    public void setTotal(int total) {
      this.total = total;
    }

    /**
     * Getter for property 'breakdown'.
     *
     * @return Value for property 'breakdown'.
     */
    public CountsByStatuses getBreakdown() {
      return breakdown;
    }

    /**
     * Setter for property 'breakdown'.
     *
     * @param breakdown Value to set for property 'breakdown'.
     */
    public void setBreakdown(CountsByStatuses breakdown) {
      this.breakdown = breakdown;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("envId", envId)
          .add("envName", envName)
          .add("total", total)
          .add("breakdown", breakdown)
          .toString();
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
      private String envId;
      private String envName;
      private int total;
      private CountsByStatuses breakdown;

      private Builder() {}

      /**
       * A breakdown by environments builder.
       *
       * @return the builder
       */
      public static Builder aBreakdownByEnvironments() {
        return new Builder();
      }

      /**
       * With env id builder.
       *
       * @param envId the env id
       * @return the builder
       */
      public Builder withEnvId(String envId) {
        this.envId = envId;
        return this;
      }

      /**
       * With env name builder.
       *
       * @param envName the env name
       * @return the builder
       */
      public Builder withEnvName(String envName) {
        this.envName = envName;
        return this;
      }

      /**
       * With total builder.
       *
       * @param total the total
       * @return the builder
       */
      public Builder withTotal(int total) {
        this.total = total;
        return this;
      }

      /**
       * With breakdown builder.
       *
       * @param breakdown the breakdown
       * @return the builder
       */
      public Builder withBreakdown(CountsByStatuses breakdown) {
        this.breakdown = breakdown;
        return this;
      }

      /**
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aBreakdownByEnvironments().withEnvId(envId).withEnvName(envName).withTotal(total).withBreakdown(
            breakdown);
      }

      /**
       * Build breakdown by environments.
       *
       * @return the breakdown by environments
       */
      public BreakdownByEnvironments build() {
        BreakdownByEnvironments breakdownByEnvironments = new BreakdownByEnvironments();
        breakdownByEnvironments.setEnvId(envId);
        breakdownByEnvironments.setEnvName(envName);
        breakdownByEnvironments.setTotal(total);
        breakdownByEnvironments.setBreakdown(breakdown);
        return breakdownByEnvironments;
      }
    }
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String releaseName;
    private String description;
    private Long targetDate;
    private Set<Service> services;
    private List<ArtifactSource> artifactSources = Lists.newArrayList();
    private List<BreakdownByEnvironments> breakdownByEnvironments;
    private Status status = Status.ACTIVE;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A release builder.
     *
     * @return the builder
     */
    public static Builder aRelease() {
      return new Builder();
    }

    /**
     * With release name builder.
     *
     * @param releaseName the release name
     * @return the builder
     */
    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With target date builder.
     *
     * @param targetDate the target date
     * @return the builder
     */
    public Builder withTargetDate(Long targetDate) {
      this.targetDate = targetDate;
      return this;
    }

    /**
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(Set<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With artifact sources builder.
     *
     * @param artifactSources the artifact sources
     * @return the builder
     */
    public Builder withArtifactSources(List<ArtifactSource> artifactSources) {
      this.artifactSources = artifactSources;
      return this;
    }

    /**
     * With breakdown by environments builder.
     *
     * @param breakdownByEnvironments the breakdown by environments
     * @return the builder
     */
    public Builder withBreakdownByEnvironments(List<BreakdownByEnvironments> breakdownByEnvironments) {
      this.breakdownByEnvironments = breakdownByEnvironments;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aRelease()
          .withReleaseName(releaseName)
          .withDescription(description)
          .withTargetDate(targetDate)
          .withServices(services)
          .withArtifactSources(artifactSources)
          .withBreakdownByEnvironments(breakdownByEnvironments)
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
     * Build release.
     *
     * @return the release
     */
    public Release build() {
      Release release = new Release();
      release.setReleaseName(releaseName);
      release.setDescription(description);
      release.setTargetDate(targetDate);
      release.setServices(services);
      release.setArtifactSources(artifactSources);
      release.setBreakdownByEnvironments(breakdownByEnvironments);
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
