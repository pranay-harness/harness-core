package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class FileMetadata {
  private String fileName;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private String relativePath;

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public ChecksumType getChecksumType() {
    return checksumType;
  }

  public void setChecksumType(ChecksumType checksumType) {
    this.checksumType = checksumType;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FileMetadata that = (FileMetadata) obj;
    return Objects.equal(fileName, that.fileName) && Objects.equal(mimeType, that.mimeType)
        && checksumType == that.checksumType && Objects.equal(checksum, that.checksum)
        && Objects.equal(relativePath, that.relativePath);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fileName, mimeType, checksumType, checksum, relativePath);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileName", fileName)
        .add("mimeType", mimeType)
        .add("checksumType", checksumType)
        .add("checksum", checksum)
        .add("relativePath", relativePath)
        .toString();
  }

  public static final class Builder {
    private String fileName;
    private String mimeType;
    private ChecksumType checksumType;
    private String checksum;
    private String relativePath;

    private Builder() {}

    public static Builder aFileMetadata() {
      return new Builder();
    }

    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
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

    public Builder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    public Builder but() {
      return aFileMetadata()
          .withFileName(fileName)
          .withMimeType(mimeType)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withRelativePath(relativePath);
    }

    public FileMetadata build() {
      FileMetadata fileMetadata = new FileMetadata();
      fileMetadata.setFileName(fileName);
      fileMetadata.setMimeType(mimeType);
      fileMetadata.setChecksumType(checksumType);
      fileMetadata.setChecksum(checksum);
      fileMetadata.setRelativePath(relativePath);
      return fileMetadata;
    }
  }
}
