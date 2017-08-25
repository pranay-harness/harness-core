package software.wings.beans.artifact;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Artifact bean class.
 *
 * @author Rishi
 */
@Entity(value = "artifacts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
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

  /**
   * Gets Artifact Path
   *
   * @return the buildNo
   */
  public String getArtifactPath() {
    if (getMetadata() != null) {
      return getMetadata().get(Constants.ARTIFACT_PATH);
    }
    return null;
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
