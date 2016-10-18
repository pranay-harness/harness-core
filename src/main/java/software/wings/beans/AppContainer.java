package software.wings.beans;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.artifact.ArtifactSource;
import software.wings.utils.ContainerFamily;
import software.wings.utils.FileType;

import java.util.Objects;

/**
 * Application bean class.
 *
 * @author Rishi
 */
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
@Entity(value = "appContainers", noClassnameStored = true)
public class AppContainer extends BaseFile {
  @FormDataParam("standard") private boolean standard;
  @FormDataParam("description") private String description;
  @FormDataParam("source") private ArtifactSource source;
  private boolean standardUpload = false;
  @FormDataParam("family") private ContainerFamily family;
  private String stackRootDirectory;
  private FileType fileType;

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

  /**
   * Getter for property 'family'.
   *
   * @return Value for property 'family'.
   */
  public ContainerFamily getFamily() {
    return family;
  }

  /**
   * Setter for property 'family'.
   *
   * @param family Value to set for property 'family'.
   */
  public void setFamily(ContainerFamily family) {
    this.family = family;
  }

  /**
   * Gets stack root directory.
   *
   * @return the stack root directory
   */
  public String getStackRootDirectory() {
    return stackRootDirectory;
  }

  /**
   * Sets stack root directory.
   *
   * @param stackRootDirectory the stack root directory
   */
  public void setStackRootDirectory(String stackRootDirectory) {
    this.stackRootDirectory = stackRootDirectory;
  }

  /**
   * Gets file type.
   *
   * @return the file type
   */
  public FileType getFileType() {
    return fileType;
  }

  /**
   * Sets file type.
   *
   * @param fileType the file type
   */
  public void setFileType(FileType fileType) {
    this.fileType = fileType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(standard, description, source, standardUpload, family, stackRootDirectory, fileType);
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
    final AppContainer other = (AppContainer) obj;
    return Objects.equals(this.standard, other.standard) && Objects.equals(this.description, other.description)
        && Objects.equals(this.source, other.source) && Objects.equals(this.standardUpload, other.standardUpload)
        && Objects.equals(this.family, other.family)
        && Objects.equals(this.stackRootDirectory, other.stackRootDirectory)
        && Objects.equals(this.fileType, other.fileType);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String fileUuid;
    private String fileName;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private boolean standard;
    private String description;
    private ArtifactSource source;
    private boolean standardUpload = false;
    private ContainerFamily family;
    private String stackRootDirectory;
    private FileType fileType;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An app container builder.
     *
     * @return the builder
     */
    public static Builder anAppContainer() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With file uuid builder.
     *
     * @param fileUuid the file uuid
     * @return the builder
     */
    public Builder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    /**
     * With file name builder.
     *
     * @param fileName the file name
     * @return the builder
     */
    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    /**
     * With mime type builder.
     *
     * @param mimeType the mime type
     * @return the builder
     */
    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With size builder.
     *
     * @param size the size
     * @return the builder
     */
    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    /**
     * With checksum type builder.
     *
     * @param checksumType the checksum type
     * @return the builder
     */
    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum builder.
     *
     * @param checksum the checksum
     * @return the builder
     */
    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With standard builder.
     *
     * @param standard the standard
     * @return the builder
     */
    public Builder withStandard(boolean standard) {
      this.standard = standard;
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
     * With source builder.
     *
     * @param source the source
     * @return the builder
     */
    public Builder withSource(ArtifactSource source) {
      this.source = source;
      return this;
    }

    /**
     * With standard upload builder.
     *
     * @param standardUpload the standard upload
     * @return the builder
     */
    public Builder withStandardUpload(boolean standardUpload) {
      this.standardUpload = standardUpload;
      return this;
    }

    /**
     * With family builder.
     *
     * @param family the family
     * @return the builder
     */
    public Builder withFamily(ContainerFamily family) {
      this.family = family;
      return this;
    }

    /**
     * With stack root directory builder.
     *
     * @param stackRootDirectory the stack root directory
     * @return the builder
     */
    public Builder withStackRootDirectory(String stackRootDirectory) {
      this.stackRootDirectory = stackRootDirectory;
      return this;
    }

    /**
     * With file type builder.
     *
     * @param fileType the file type
     * @return the builder
     */
    public Builder withFileType(FileType fileType) {
      this.fileType = fileType;
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
      return anAppContainer()
          .withName(name)
          .withFileUuid(fileUuid)
          .withFileName(fileName)
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withStandard(standard)
          .withDescription(description)
          .withSource(source)
          .withStandardUpload(standardUpload)
          .withFamily(family)
          .withStackRootDirectory(stackRootDirectory)
          .withFileType(fileType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build app container.
     *
     * @return the app container
     */
    public AppContainer build() {
      AppContainer appContainer = new AppContainer();
      appContainer.setName(name);
      appContainer.setFileUuid(fileUuid);
      appContainer.setFileName(fileName);
      appContainer.setMimeType(mimeType);
      appContainer.setSize(size);
      appContainer.setChecksumType(checksumType);
      appContainer.setChecksum(checksum);
      appContainer.setStandard(standard);
      appContainer.setDescription(description);
      appContainer.setSource(source);
      appContainer.setStandardUpload(standardUpload);
      appContainer.setFamily(family);
      appContainer.setStackRootDirectory(stackRootDirectory);
      appContainer.setFileType(fileType);
      appContainer.setUuid(uuid);
      appContainer.setAppId(appId);
      appContainer.setCreatedBy(createdBy);
      appContainer.setCreatedAt(createdAt);
      appContainer.setLastUpdatedBy(lastUpdatedBy);
      appContainer.setLastUpdatedAt(lastUpdatedAt);
      return appContainer;
    }
  }
}
