package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;

public class ArtifactFile extends BaseFile {
  private List<Service> services;

  public List<Service> getServices() {
    return services;
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ArtifactFile that = (ArtifactFile) o;
    return Objects.equal(services, that.services);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), services);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("services", services).toString();
  }

  public static final class Builder {
    private List<Service> services;
    private String fileUuid;
    private String name;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anArtifactFile() {
      return new Builder();
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    public Builder withFileUuid(String fileUuid) {
      this.fileUuid = fileUuid;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
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
      return anArtifactFile()
          .withServices(services)
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

    public ArtifactFile build() {
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setServices(services);
      artifactFile.setFileUuid(fileUuid);
      artifactFile.setName(name);
      artifactFile.setMimeType(mimeType);
      artifactFile.setSize(size);
      artifactFile.setChecksumType(checksumType);
      artifactFile.setChecksum(checksum);
      artifactFile.setUuid(uuid);
      artifactFile.setAppId(appId);
      artifactFile.setCreatedBy(createdBy);
      artifactFile.setCreatedAt(createdAt);
      artifactFile.setLastUpdatedBy(lastUpdatedBy);
      artifactFile.setLastUpdatedAt(lastUpdatedAt);
      artifactFile.setActive(active);
      return artifactFile;
    }
  }
}
