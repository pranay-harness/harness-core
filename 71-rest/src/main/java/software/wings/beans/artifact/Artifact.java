package software.wings.beans.artifact;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Indexes({
  @Index(options = @IndexOptions(name = "owners"), fields = { @Field("artifactStreamId")
                                                              , @Field("appId") })
  , @Index(options = @IndexOptions(name = "artifactStream_buildNo"), fields = {
    @Field("artifactStreamId"), @Field("metadata.buildNo")
  }), @Index(options = @IndexOptions(name = "artifactStream_artifactPath"), fields = {
    @Field("artifactStreamId"), @Field("metadata.artifactPath")
  }), @Index(options = @IndexOptions(name = "artifactStream_revision"), fields = {
    @Field("artifactStreamId"), @Field("revision")
  })
})
@FieldNameConstants(innerTypeName = "ArtifactKeys")
@Entity(value = "artifacts", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Artifact extends Base {
  @UtilityClass
  public static final class ArtifactMetadataKeys {
    public static final String artifactFileName = "artifactFileName";
    public static final String artifactFileSize = "artifactFileSize";
    public static final String artifactPath = "artifactPath";
    public static final String bucketName = "bucketName";
    public static final String buildFullDisplayName = "buildFullDisplayName";
    public static final String buildNo = "buildNo";
    public static final String key = "key";
    public static final String url = "url";
    public static final String image = "image";
    public static final String tag = "tag";
    public static final String repositoryName = "repositoryName";
    public static final String nexusPackageName = "package";
    public static final String version = "version";
    public static final String nexusGroupId = "groupId";
    public static final String nexusArtifactId = "artifactId";
    public static final String versionId = "versionId";
    public static final String publishDate = "publishDate";
  }

  @UtilityClass
  public static final class ArtifactKeys {
    public static final String uuid = "uuid";
    public static final String appId = "appId";
    public static final String metadata_image = metadata + "." + ArtifactMetadataKeys.image;
    public static final String metadata_tag = metadata + "." + ArtifactMetadataKeys.tag;
    public static final String metadata_buildNo = metadata + "." + ArtifactMetadataKeys.buildNo;
    public static final String metadata_artifactPath = metadata + "." + ArtifactMetadataKeys.artifactPath;
  }

  private String artifactStreamId;
  private String artifactSourceName;
  private Map<String, String> metadata = Maps.newHashMap();
  private Map<String, String> labels = Maps.newHashMap();
  @NotEmpty private String displayName;
  private String revision;
  private List<String> serviceIds = new ArrayList<>();
  @Transient private List<Service> services;
  private List<ArtifactFile> artifactFiles = Lists.newArrayList();
  private List<ArtifactFileMetadata> artifactFileMetadata = Lists.newArrayList();
  private Status status;
  private String description;
  private String errorMessage;
  private ContentStatus contentStatus;
  private Map<String, String> source;
  private String settingId;
  private String accountId;
  private String artifactStreamType;
  private String uiDisplayName;
  private String buildIdentity;

  /**
   * Gets buildNo.
   *
   * @return the buildNo
   */
  public String getBuildNo() {
    if (getMetadata() != null) {
      return getMetadata().get(ArtifactMetadataKeys.buildNo);
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
      return getMetadata().get(ArtifactMetadataKeys.artifactPath);
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
      return getMetadata().get(ArtifactMetadataKeys.bucketName);
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
      return getMetadata().get(ArtifactMetadataKeys.key);
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
      return getMetadata().get(ArtifactMetadataKeys.url);
    }
    return null;
  }

  /**
   * Gets artifact file Name
   * @return
   */
  public String getFileName() {
    if (getMetadata() != null) {
      return getMetadata().get(ArtifactMetadataKeys.artifactFileName);
    }
    return null;
  }

  // TODO: ASR: IMP: remove this method after refactor
  public String fetchAppId() {
    return appId;
  }

  /**
   * Gets Full build display name
   * @return
   */
  public String getBuildFullDisplayName() {
    if (getMetadata() != null) {
      return getMetadata().get(ArtifactMetadataKeys.buildFullDisplayName);
    }
    return null;
  }

  /**
   * Gets Artifact file size
   *
   * @return
   */
  public Long getArtifactFileSize() {
    if (getMetadata() != null && getMetadata().get(ArtifactMetadataKeys.artifactFileSize) != null) {
      return Long.valueOf(getMetadata().get(ArtifactMetadataKeys.artifactFileSize));
    }
    return null;
  }

  public String getUiDisplayName() {
    if (EmptyPredicate.isNotEmpty(uiDisplayName)) {
      return uiDisplayName;
    }
    return "Build# " + getBuildNo();
  }

  public List<ArtifactFileMetadata> getArtifactFileMetadata() {
    return artifactFileMetadata;
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
    private Map<String, String> labels = Maps.newHashMap();
    private String displayName;
    private String revision;
    private List<String> serviceIds = new ArrayList<>();
    private String uuid;
    private List<Service> services;
    private List<ArtifactFile> artifactFiles = Lists.newArrayList();
    private List<ArtifactFileMetadata> artifactFileMetadata = Lists.newArrayList();
    private EmbeddedUser createdBy;
    private long createdAt;
    private Status status;
    private String description;
    private EmbeddedUser lastUpdatedBy;
    private String errorMessage;
    private long lastUpdatedAt;
    private ContentStatus contentStatus;
    private String settingId;
    private String accountId;
    private String artifactStreamType;
    private String uiDisplayName;

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

    public Builder withLabels(Map<String, String> labels) {
      this.labels = labels;
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

    public Builder withArtifactDownloadMetadata(List<ArtifactFileMetadata> artifactFileMetadata) {
      this.artifactFileMetadata = artifactFileMetadata;
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

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withArtifactStreamType(String artifactStreamType) {
      this.artifactStreamType = artifactStreamType;
      return this;
    }

    public Builder withUiDisplayName(String uiDisplayName) {
      this.uiDisplayName = uiDisplayName;
      return this;
    }

    public Builder but() {
      return anArtifact()
          .withArtifactStreamId(artifactStreamId)
          .withArtifactSourceName(artifactSourceName)
          .withMetadata(metadata)
          .withLabels(labels)
          .withDisplayName(displayName)
          .withRevision(revision)
          .withServiceIds(serviceIds)
          .withUuid(uuid)
          .withAppId(appId)
          .withServices(services)
          .withArtifactFiles(artifactFiles)
          .withArtifactDownloadMetadata(artifactFileMetadata)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withStatus(status)
          .withDescription(description)
          .withLastUpdatedBy(lastUpdatedBy)
          .withErrorMessage(errorMessage)
          .withLastUpdatedAt(lastUpdatedAt)
          .withContentStatus(contentStatus)
          .withEntityYamlPath(entityYamlPath)
          .withSettingId(settingId)
          .withAccountId(accountId)
          .withArtifactStreamType(artifactStreamType)
          .withUiDisplayName(uiDisplayName);
    }

    public Artifact build() {
      Artifact artifact = new Artifact();
      artifact.setArtifactStreamId(artifactStreamId);
      artifact.setArtifactSourceName(artifactSourceName);
      artifact.setMetadata(metadata);
      artifact.setLabels(labels);
      artifact.setDisplayName(displayName);
      artifact.setRevision(revision);
      artifact.setServiceIds(serviceIds);
      artifact.setUuid(uuid);
      artifact.setAppId(appId);
      artifact.setServices(services);
      artifact.setArtifactFiles(artifactFiles);
      artifact.setArtifactFileMetadata(artifactFileMetadata);
      artifact.setCreatedBy(createdBy);
      artifact.setCreatedAt(createdAt);
      artifact.setStatus(status);
      artifact.setDescription(description);
      artifact.setLastUpdatedBy(lastUpdatedBy);
      artifact.setErrorMessage(errorMessage);
      artifact.setLastUpdatedAt(lastUpdatedAt);
      artifact.setContentStatus(contentStatus);
      artifact.setEntityYamlPath(entityYamlPath);
      artifact.setSettingId(settingId);
      artifact.setAccountId(accountId);
      artifact.setArtifactStreamType(artifactStreamType);
      artifact.setUiDisplayName(uiDisplayName);
      return artifact;
    }
  }
}
