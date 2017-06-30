package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Artifact bean class.
 *
 * @author Rishi
 */
@Entity(value = "artifacts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Artifact extends Base {
  @Indexed private String artifactStreamId;

  private String artifactSourceName;

  private Map<String, String> metadata = Maps.newHashMap();

  @Indexed @NotEmpty private String displayName;

  @Indexed private String revision;

  private List<String> serviceIds = new ArrayList<>();

  @Transient private List<Service> services;

  private List<ArtifactFile> artifactFiles = Lists.newArrayList();

  @Indexed private Status status;

  private String description;

  /**
   * Gets metadata.
   *
   * @return the metadata
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Sets metadata.
   *
   * @param metadata the metadata
   */
  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets revision.
   *
   * @return the revision
   */
  public String getRevision() {
    return revision;
  }

  /**
   * Sets revision.
   *
   * @param revision the revision
   */
  public void setRevision(String revision) {
    this.revision = revision;
  }

  /**
   * Gets buildNo.
   *
   * @return the buildNo
   */
  public String getBuildNo() {
    if (getMetadata() != null) {
      return getMetadata().get(Constants.BUILD_NO);
    }
    return null;
  }

  public Map<String, String> getBuildParameters() {
    if (getMetadata() != null) {
      return getMetadata();
    }
    return new HashMap<>();
  }

  /**
   * Sets buildNo.
   *
   * @param buildNo the buildNo
   */
  public void setBuildNo(String buildNo) {}

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
   * Gets artifact files.
   *
   * @return the artifact files
   */
  public List<ArtifactFile> getArtifactFiles() {
    return artifactFiles;
  }

  /**
   * Sets artifact files.
   *
   * @param artifactFiles the artifact files
   */
  public void setArtifactFiles(List<ArtifactFile> artifactFiles) {
    this.artifactFiles = artifactFiles;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public List<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<Service> services) {
    this.services = services;
  }

  /**
   * Gets artifact source id.
   *
   * @return the artifact source id
   */
  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  /**
   * Sets artifact source id.
   *
   * @param artifactStreamId the artifact source id
   */
  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  /**
   * Gets artifact source name.
   *
   * @return the artifact source name
   */
  public String getArtifactSourceName() {
    return artifactSourceName;
  }

  /**
   * Sets artifact source name.
   *
   * @param artifactSourceName the artifact source name
   */
  public void setArtifactSourceName(String artifactSourceName) {
    this.artifactSourceName = artifactSourceName;
  }

  /**
   * Gets service ids.
   *
   * @return the service ids
   */
  public List<String> getServiceIds() {
    return serviceIds;
  }

  /**
   * Sets service ids.
   *
   * @param serviceIds the service ids
   */
  public void setServiceIds(List<String> serviceIds) {
    this.serviceIds = serviceIds;
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
   * The Enum Status.
   */
  public enum Status {
    /**
     * New status.
     */
    NEW, /**
          * Running status.
          */
    RUNNING, /**
              * Queued status.
              */
    QUEUED, /**
             * Waiting status.
             */
    WAITING, /**
              * Ready status.
              */
    READY, /**
            * Aborted status.
            */
    APPROVED, /**
               * Rejected status.
               */
    REJECTED, /**
               * Aborted status.
               */
    ABORTED, /**
              * Failed status.
              */
    FAILED, /**
             * Error status.
             */
    ERROR
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(artifactStreamId, metadata, displayName, revision, serviceIds, services, artifactFiles, status);
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
    final Artifact other = (Artifact) obj;
    return Objects.equals(this.artifactStreamId, other.artifactStreamId)
        && Objects.equals(this.metadata, other.metadata) && Objects.equals(this.displayName, other.displayName)
        && Objects.equals(this.revision, other.revision) && Objects.equals(this.serviceIds, other.serviceIds)
        && Objects.equals(this.services, other.services) && Objects.equals(this.artifactFiles, other.artifactFiles)
        && Objects.equals(this.status, other.status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifactStreamId", artifactStreamId)
        .add("metadata", metadata)
        .add("displayName", displayName)
        .add("revision", revision)
        .add("serviceIds", serviceIds)
        .add("services", services)
        .add("artifactFiles", artifactFiles)
        .add("status", status)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String artifactStreamId;
    private String artifactSourceName;
    private Map<String, String> metadata = Maps.newHashMap();
    private String displayName;
    private String revision;
    private List<String> serviceIds = new ArrayList<>();
    private List<Service> services;
    private List<ArtifactFile> artifactFiles = Lists.newArrayList();
    private Status status;
    private String description;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An artifact builder.
     *
     * @return the builder
     */
    public static Builder anArtifact() {
      return new Builder();
    }

    /**
     * With artifact stream id builder.
     *
     * @param artifactStreamId the artifact stream id
     * @return the builder
     */
    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    /**
     * With artifact source name builder.
     *
     * @param artifactSourceName the artifact source name
     * @return the builder
     */
    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    /**
     * With metadata builder.
     *
     * @param metadata the metadata
     * @return the builder
     */
    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * With display name builder.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * With revision builder.
     *
     * @param revision the revision
     * @return the builder
     */
    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * With service ids builder.
     *
     * @param serviceIds the service ids
     * @return the builder
     */
    public Builder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    /**
     * With services builder.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With artifact files builder.
     *
     * @param artifactFiles the artifact files
     * @return the builder
     */
    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
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
    public Builder withCreatedBy(EmbeddedUser createdBy) {
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifact()
          .withArtifactStreamId(artifactStreamId)
          .withArtifactSourceName(artifactSourceName)
          .withMetadata(metadata)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withServiceIds(serviceIds)
          .withServices(services)
          .withArtifactFiles(artifactFiles)
          .withStatus(status)
          .withDescription(description)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build artifact.
     *
     * @return the artifact
     */
    public Artifact build() {
      Artifact artifact = new Artifact();
      artifact.setArtifactStreamId(artifactStreamId);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setMetadata(metadata);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setServiceIds(serviceIds);
      artifact.setServices(services);
      artifact.setArtifactFiles(artifactFiles);
      artifact.setStatus(status);
      artifact.setDescription(description);
      artifact.setUuid(uuid);
      artifact.setAppId(appId);
      artifact.setCreatedBy(createdBy);
      artifact.setCreatedAt(createdAt);
      artifact.setLastUpdatedBy(lastUpdatedBy);
      artifact.setLastUpdatedAt(lastUpdatedAt);
      return artifact;
    }
  }
}
