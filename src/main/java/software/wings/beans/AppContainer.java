package software.wings.beans;

import static software.wings.beans.ChecksumType.MD5;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

/**
 * Application bean class.
 *
 * @author Rishi
 */
@Indexes(
    @Index(fields = { @Field("appId")
                      , @Field("name"), @Field("version") }, options = @IndexOptions(unique = true)))
@Entity(value = "appContainers", noClassnameStored = true)
public class AppContainer extends BaseFile {
  @FormDataParam("standard") private boolean standard;
  @FormDataParam("version") private String version;
  @FormDataParam("description") private String description;
  @FormDataParam("source") private ArtifactSource source;
  private boolean standardUpload = false;

  /**
   * Instantiates a new app container.
   */
  public AppContainer() {}

  /**
   * Instantiates a new app container.
   *
   * @param fileName the file name
   * @param md5      the md5
   */
  public AppContainer(String fileName, String md5) {
    super(fileName, md5);
  }

  /**
   * Is standard boolean.
   *
   * @return the boolean
   */
  public boolean isStandard() {
    return standard;
  }

  /**
   * Sets standard.
   *
   * @param standard the standard
   */
  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  /**
   * Gets version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets version.
   *
   * @param version the version
   */
  public void setVersion(String version) {
    this.version = version;
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
   * Gets source.
   *
   * @return the source
   */
  public ArtifactSource getSource() {
    return source;
  }

  /**
   * Sets source.
   *
   * @param source the source
   */
  public void setSource(ArtifactSource source) {
    this.source = source;
  }

  /**
   * Is standard upload boolean.
   *
   * @return the boolean
   */
  public boolean isStandardUpload() {
    return standardUpload;
  }

  /**
   * Sets standard upload.
   *
   * @param standardUpload the standard upload
   */
  public void setStandardUpload(boolean standardUpload) {
    this.standardUpload = standardUpload;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.BaseFile#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(standard, version, description, source, standardUpload);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.BaseFile#equals(java.lang.Object)
   */
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
    final AppContainer other = (AppContainer) obj;
    return Objects.equals(this.standard, other.standard) && Objects.equals(this.version, other.version)
        && Objects.equals(this.description, other.description) && Objects.equals(this.source, other.source)
        && Objects.equals(this.standardUpload, other.standardUpload);
  }

  /**
   * The Class AppContainerBuilder.
   */
  public static final class AppContainerBuilder {
    private boolean standard;
    private String version;
    private String description;
    private ArtifactSource source;
    private boolean standardUpload = false;
    private String fileUuid;
    private String name;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private AppContainerBuilder() {}

    /**
     * An app container.
     *
     * @return the app container builder
     */
    public static AppContainerBuilder anAppContainer() {
      return new AppContainerBuilder();
    }

    /**
     * With standard.
     *
     * @param standard the standard
     * @return the app container builder
     */
    public AppContainerBuilder withStandard(boolean standard) {
      this.standard = standard;
      return this;
    }

    /**
     * With version.
     *
     * @param version the version
     * @return the app container builder
     */
    public AppContainerBuilder withVersion(String version) {
      this.version = version;
      return this;
    }

    /**
     * With description.
     *
     * @param description the description
     * @return the app container builder
     */
    public AppContainerBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With source.
     *
     * @param source the source
     * @return the app container builder
     */
    public AppContainerBuilder withSource(ArtifactSource source) {
      this.source = source;
      return this;
    }

    /**
     * With standard upload.
     *
     * @param standardUpload the standard upload
     * @return the app container builder
     */
    public AppContainerBuilder withStandardUpload(boolean standardUpload) {
      this.standardUpload = standardUpload;
      return this;
    }

    /**
     * With file uuid.
     *
     * @param fileUuid the file uuid
     * @return the app container builder
     */
    public AppContainerBuilder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the app container builder
     */
    public AppContainerBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With mime type.
     *
     * @param mimeType the mime type
     * @return the app container builder
     */
    public AppContainerBuilder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size.
     *
     * @param size the size
     * @return the app container builder
     */
    public AppContainerBuilder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type.
     *
     * @param checksumType the checksum type
     * @return the app container builder
     */
    public AppContainerBuilder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum.
     *
     * @param checksum the checksum
     * @return the app container builder
     */
    public AppContainerBuilder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the app container builder
     */
    public AppContainerBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the app container builder
     */
    public AppContainerBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the app container builder
     */
    public AppContainerBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the app container builder
     */
    public AppContainerBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the app container builder
     */
    public AppContainerBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the app container builder
     */
    public AppContainerBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the app container builder
     */
    public AppContainerBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the app container builder
     */
    public AppContainerBuilder but() {
      return anAppContainer()
          .withStandard(standard)
          .withVersion(version)
          .withDescription(description)
          .withSource(source)
          .withStandardUpload(standardUpload)
          .withFileUuid(fileUuid)
          .withName(name)
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
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
     * @return the app container
     */
    public AppContainer build() {
      AppContainer appContainer = new AppContainer();
      appContainer.setStandard(standard);
      appContainer.setVersion(version);
      appContainer.setDescription(description);
      appContainer.setSource(source);
      appContainer.setStandardUpload(standardUpload);
      appContainer.setFileUuid(fileUuid);
      appContainer.setName(name);
      appContainer.setMimeType(mimeType);
      appContainer.setSize(size);
      appContainer.setChecksumType(checksumType);
      appContainer.setChecksum(checksum);
      appContainer.setUuid(uuid);
      appContainer.setAppId(appId);
      appContainer.setCreatedBy(createdBy);
      appContainer.setCreatedAt(createdAt);
      appContainer.setLastUpdatedBy(lastUpdatedBy);
      appContainer.setLastUpdatedAt(lastUpdatedAt);
      appContainer.setActive(active);
      return appContainer;
    }
  }
}
