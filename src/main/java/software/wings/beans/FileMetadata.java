package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The Class FileMetadata.
 */
public class FileMetadata {
  private String fileName;
  private String mimeType;
  private ChecksumType checksumType;
  private String checksum;
  private String relativePath;

  /**
   * Gets file name.
   *
   * @return the file name
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Sets file name.
   *
   * @param fileName the file name
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Gets mime type.
   *
   * @return the mime type
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets mime type.
   *
   * @param mimeType the mime type
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * Gets checksum type.
   *
   * @return the checksum type
   */
  public ChecksumType getChecksumType() {
    return checksumType;
  }

  /**
   * Sets checksum type.
   *
   * @param checksumType the checksum type
   */
  public void setChecksumType(ChecksumType checksumType) {
    this.checksumType = checksumType;
  }

  /**
   * Gets checksum.
   *
   * @return the checksum
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * Sets checksum.
   *
   * @param checksum the checksum
   */
  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  /**
   * Gets relative path.
   *
   * @return the relative path
   */
  public String getRelativePath() {
    return relativePath;
  }

  /**
   * Sets relative path.
   *
   * @param relativePath the relative path
   */
  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(fileName, mimeType, checksumType, checksum, relativePath);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String fileName;
    private String mimeType;
    private ChecksumType checksumType;
    private String checksum;
    private String relativePath;

    private Builder() {}

    /**
     * A file metadata.
     *
     * @return the builder
     */
    public static Builder aFileMetadata() {
      return new Builder();
    }

    /**
     * With file name.
     *
     * @param fileName the file name
     * @return the builder
     */
    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    /**
     * With mime type.
     *
     * @param mimeType the mime type
     * @return the builder
     */
    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * With checksum type.
     *
     * @param checksumType the checksum type
     * @return the builder
     */
    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    /**
     * With checksum.
     *
     * @param checksum the checksum
     * @return the builder
     */
    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    /**
     * With relative path.
     *
     * @param relativePath the relative path
     * @return the builder
     */
    public Builder withRelativePath(String relativePath) {
      this.relativePath = relativePath;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aFileMetadata()
          .withFileName(fileName)
          .withMimeType(mimeType)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withRelativePath(relativePath);
    }

    /**
     * Builds the.
     *
     * @return the file metadata
     */
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
