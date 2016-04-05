package software.wings.beans;

import com.google.common.base.MoreObjects;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import java.util.Objects;

/**
 *  Application bean class.
 *
 *
 * @author Rishi
 *
 */

@Entity(value = "platforms", noClassnameStored = true)
public class PlatformSoftware extends Base {
  @Reference(idOnly = true) private Application application;

  private boolean standard;
  private String name;
  private String version;
  private String description;

  private String md5;
  private ArtifactSource source;
  private String binaryDocumentId;

  public boolean isStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBinaryDocumentId() {
    return binaryDocumentId;
  }

  public void setBinaryDocumentId(String binaryDocumentId) {
    this.binaryDocumentId = binaryDocumentId;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public String getMd5() {
    return md5;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public ArtifactSource getSource() {
    return source;
  }

  public void setSource(ArtifactSource source) {
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    PlatformSoftware that = (PlatformSoftware) o;
    return standard == that.standard && Objects.equals(application, that.application) && Objects.equals(name, that.name)
        && Objects.equals(version, that.version) && Objects.equals(description, that.description)
        && Objects.equals(md5, that.md5) && Objects.equals(source, that.source)
        && Objects.equals(binaryDocumentId, that.binaryDocumentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), application, standard, name, version, description, md5, source, binaryDocumentId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("application", application)
        .add("standard", standard)
        .add("name", name)
        .add("version", version)
        .add("description", description)
        .add("md5", md5)
        .add("source", source)
        .add("binaryDocumentId", binaryDocumentId)
        .toString();
  }

  public static class Builder {
    private Application application;
    private boolean standard;
    private String name;
    private String version;
    private String description;
    private String md5;
    private ArtifactSource source;
    private String binaryDocumentId;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aPlatformSoftware() {
      return new Builder();
    }

    public Builder withApplication(Application application) {
      this.application = application;
      return this;
    }

    public Builder withStandard(boolean standard) {
      this.standard = standard;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
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

    public Builder withMd5(String md5) {
      this.md5 = md5;
      return this;
    }

    public Builder withSource(ArtifactSource source) {
      this.source = source;
      return this;
    }

    public Builder withBinaryDocumentId(String binaryDocumentId) {
      this.binaryDocumentId = binaryDocumentId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
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
      return aPlatformSoftware()
          .withApplication(application)
          .withStandard(standard)
          .withName(name)
          .withVersion(version)
          .withDescription(description)
          .withMd5(md5)
          .withSource(source)
          .withBinaryDocumentId(binaryDocumentId)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public PlatformSoftware build() {
      PlatformSoftware platformSoftware = new PlatformSoftware();
      platformSoftware.setApplication(application);
      platformSoftware.setStandard(standard);
      platformSoftware.setName(name);
      platformSoftware.setVersion(version);
      platformSoftware.setDescription(description);
      platformSoftware.setMd5(md5);
      platformSoftware.setSource(source);
      platformSoftware.setBinaryDocumentId(binaryDocumentId);
      platformSoftware.setUuid(uuid);
      platformSoftware.setCreatedBy(createdBy);
      platformSoftware.setCreatedAt(createdAt);
      platformSoftware.setLastUpdatedBy(lastUpdatedBy);
      platformSoftware.setLastUpdatedAt(lastUpdatedAt);
      platformSoftware.setActive(active);
      return platformSoftware;
    }
  }
}
