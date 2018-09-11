package software.wings.beans.artifact;

import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_FILE_SIZE;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUCKET_NAME;
import static software.wings.common.Constants.BUILD_FULL_DISPLAY_NAME;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.KEY;
import static software.wings.common.Constants.URL;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Service;

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
@Indexes(@Index(options = @IndexOptions(name = "owners"), fields = { @Field("artifactStreamId")
                                                                     , @Field("appId") }))
public class Artifact extends Base {
  public static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";
  public static final String CONTENT_STATUS_KEY = "contentStatus";
  public static final String STATUS_KEY = "status";
  public static final String ERROR_MSG_KEY = "errorMessage";
  public static final String DISPLAY_NAME_KEY = "displayName";
  public static final String SERVICE_ID_KEY = "serviceIds";

  @Indexed private String artifactStreamId;
  @Indexed private String artifactSourceName;
  private Map<String, String> metadata = Maps.newHashMap();
  @Indexed @NotEmpty private String displayName;
  @Indexed private String revision;
  private List<String> serviceIds = new ArrayList<>();
  @Transient private List<Service> services;
  private List<ArtifactFile> artifactFiles = Lists.newArrayList();
  @Indexed private Status status;
  private String description;
  private String errorMessage;
  @Indexed private ContentStatus contentStatus;
  transient Map<String, String> source;

  /**
   * Gets buildNo.
   *
   * @return the buildNo
   */
  public String getBuildNo() {
    if (getMetadata() != null) {
      return getMetadata().get(BUILD_NO);
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
      return getMetadata().get(ARTIFACT_PATH);
    }
    return null;
  }

  /**
   * Gets Bucket Name
   *
   * @return the bucket name
   */
  public String getBucketName() {
    if (getMetadata() != null) {
      return getMetadata().get(BUCKET_NAME);
    }
    return null;
  }

  /**
   * Gets Key
   *
   * @return the bucket name
   */
  public String getKey() {
    if (getMetadata() != null) {
      return getMetadata().get(KEY);
    }
    return null;
  }

  /**
   * Gets Key
   *
   * @return the bucket name
   */
  public String getUrl() {
    if (getMetadata() != null) {
      return getMetadata().get(URL);
    }
    return null;
  }

  /**
   * Gets artifact file Name
   * @return
   */
  public String getArtifactFileName() {
    if (getMetadata() != null) {
      return getMetadata().get(ARTIFACT_FILE_NAME);
    }
    return null;
  }

  /**
   * Gets Full build display name
   * @return
   */
  public String getBuildFullDisplayName() {
    if (getMetadata() != null) {
      return getMetadata().get(BUILD_FULL_DISPLAY_NAME);
    }
    return null;
  }

  /**
   * Gets Artifact file size
   *
   * @return
   */
  public Long getArtifactFileSize() {
    if (getMetadata() != null && getMetadata().get(ARTIFACT_FILE_SIZE) != null) {
      return Long.valueOf(getMetadata().get(ARTIFACT_FILE_SIZE));
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
    NEW,
    /**
     * Running status.
     */
    RUNNING,
    /**
     * Queued status.
     */
    QUEUED,
    /**
     * Waiting status.
     */
    WAITING,
    /**
     * Ready status.
     */
    READY(true),
    /**
     * Aborted status.
     */
    APPROVED(true),
    /**
     * Rejected status.
     */
    REJECTED(true),
    /**
     * Aborted status.
     */
    ABORTED(true),
    /**
     * Failed status.
     */
    FAILED(true),
    /**
     * Error status.
     */
    ERROR(true);

    Status() {}

    Status(boolean finalStatus) {
      this.finalStatus = finalStatus;
    }

    public boolean isFinalStatus() {
      return finalStatus;
    }

    private boolean finalStatus;
  }

  /**
   * The Enum Status.
   */
  public enum ContentStatus {
    /***
     * METADATA ONLY
     */
    METADATA_ONLY,
    /**
     * New status.
     */
    NOT_DOWNLOADED,
    /**
     * Downloading status.
     */
    DOWNLOADING,
    /**
     * Downloaded status.
     */
    DOWNLOADED(true),
    /**
     * Waiting status.
     */
    DELETED(true),

    /**
     * Failed status
     *
     */
    FAILED(true);

    ContentStatus() {}

    ContentStatus(boolean finalStatus) {
      this.finalStatus = finalStatus;
    }

    public boolean isFinalStatus() {
      return finalStatus;
    }

    private boolean finalStatus;
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String artifactStreamId;
    private String artifactSourceName;
    private Map<String, String> metadata = Maps.newHashMap();
    private String displayName;
    private String revision;
    private List<String> serviceIds = new ArrayList<>();
    private String uuid;
    private List<Service> services;
    private List<ArtifactFile> artifactFiles = Lists.newArrayList();
    private EmbeddedUser createdBy;
    private long createdAt;
    private Status status;
    private String description;
    private EmbeddedUser lastUpdatedBy;
    private String errorMessage;
    private long lastUpdatedAt;
    private ContentStatus contentStatus;
    private List<String> keywords;

    private Builder() {}

    public static Builder anArtifact() {
      return new Builder();
    }

    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    public Builder withArtifactSourceName(String artifactSourceName) {
      this.artifactSourceName = artifactSourceName;
      return this;
    }

    public Builder withMetadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public Builder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public Builder withArtifactFiles(List<ArtifactFile> artifactFiles) {
      this.artifactFiles = artifactFiles;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withContentStatus(ContentStatus contentStatus) {
      this.contentStatus = contentStatus;
      return this;
    }

    public Builder withKeywords(List<String> keywords) {
      this.keywords = keywords;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder but() {
      return anArtifact()
          .withArtifactStreamId(artifactStreamId)
          .withArtifactSourceName(artifactSourceName)
          .withMetadata(metadata)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withServiceIds(serviceIds)
          .withUuid(uuid)
          .withAppId(appId)
          .withServices(services)
          .withArtifactFiles(artifactFiles)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withStatus(status)
          .withDescription(description)
          .withLastUpdatedBy(lastUpdatedBy)
          .withErrorMessage(errorMessage)
          .withLastUpdatedAt(lastUpdatedAt)
          .withContentStatus(contentStatus)
          .withKeywords(keywords)
          .withEntityYamlPath(entityYamlPath);
    }

    public Artifact build() {
      Artifact artifact = new Artifact();
      artifact.setArtifactStreamId(artifactStreamId);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setMetadata(metadata);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setServiceIds(serviceIds);
      artifact.setUuid(uuid);
      artifact.setAppId(appId);
      artifact.setServices(services);
      artifact.setArtifactFiles(artifactFiles);
      artifact.setCreatedBy(createdBy);
      artifact.setCreatedAt(createdAt);
      artifact.setStatus(status);
      artifact.setDescription(description);
      artifact.setLastUpdatedBy(lastUpdatedBy);
      artifact.setErrorMessage(errorMessage);
      artifact.setLastUpdatedAt(lastUpdatedAt);
      artifact.setContentStatus(contentStatus);
      artifact.setKeywords(keywords);
      artifact.setEntityYamlPath(entityYamlPath);
      return artifact;
    }
  }
}
