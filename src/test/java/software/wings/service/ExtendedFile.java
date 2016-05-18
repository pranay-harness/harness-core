package software.wings.service;

import org.junit.Ignore;
import software.wings.beans.BaseFile;
import software.wings.beans.ChecksumType;
import software.wings.beans.User;

/**
 * Created by peeyushaggarwal on 5/17/16.
 */
@Ignore
public class ExtendedFile extends BaseFile {
  public static final class Builder {
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

    public static Builder anExtendedFile() {
      return new Builder();
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
      return anExtendedFile()
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

    public ExtendedFile build() {
      ExtendedFile extendedFile = new ExtendedFile();
      extendedFile.setFileUuid(fileUuid);
      extendedFile.setName(name);
      extendedFile.setMimeType(mimeType);
      extendedFile.setSize(size);
      extendedFile.setChecksumType(checksumType);
      extendedFile.setChecksum(checksum);
      extendedFile.setUuid(uuid);
      extendedFile.setAppId(appId);
      extendedFile.setCreatedBy(createdBy);
      extendedFile.setCreatedAt(createdAt);
      extendedFile.setLastUpdatedBy(lastUpdatedBy);
      extendedFile.setLastUpdatedAt(lastUpdatedAt);
      extendedFile.setActive(active);
      return extendedFile;
    }
  }
}
