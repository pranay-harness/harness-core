package software.wings.beans;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.utils.ContainerFamily;

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
  @FormDataParam("family") private ContainerFamily family;

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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(standard, version, description, source, standardUpload, family);
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
    return Objects.equals(this.standard, other.standard) && Objects.equals(this.version, other.version)
        && Objects.equals(this.description, other.description) && Objects.equals(this.source, other.source)
        && Objects.equals(this.standardUpload, other.standardUpload) && Objects.equals(this.family, other.family);
  }

  public static final class Builder {
    private String name;
    private String fileUuid;
    private String fileName;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private boolean standard;
    private String version;
    private String description;
    private ArtifactSource source;
    private boolean standardUpload = false;
    private ContainerFamily family;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anAppContainer() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder withStandard(boolean standard) {
      this.standard = standard;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withSource(ArtifactSource source) {
      this.source = source;
      return this;
    }

    public Builder withStandardUpload(boolean standardUpload) {
      this.standardUpload = standardUpload;
      return this;
    }

    public Builder withFamily(ContainerFamily family) {
      this.family = family;
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

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

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
          .withVersion(version)
          .withDescription(description)
          .withSource(source)
          .withStandardUpload(standardUpload)
          .withFamily(family)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

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
      appContainer.setVersion(version);
      appContainer.setDescription(description);
      appContainer.setSource(source);
      appContainer.setStandardUpload(standardUpload);
      appContainer.setFamily(family);
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
