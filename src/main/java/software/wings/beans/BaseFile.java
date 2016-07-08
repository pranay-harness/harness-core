package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/13/16.
 */
public class BaseFile extends Base {
  private String fileUuid;
  @FormDataParam("name") private String name;
  private String mimeType;
  private long size;
  private ChecksumType checksumType = ChecksumType.MD5;
  @FormDataParam("md5") private String checksum;

  /**
   * Instantiates a new base file.
   */
  public BaseFile() {}

  /**
   * Instantiates a new base file.
   *
   * @param fileName the file name
   * @param md5      the md5
   */
  public BaseFile(String fileName, String md5) {
    this.name = fileName;
    if (StringUtils.isNotBlank(md5)) {
      this.checksum = md5;
    }
  }

  /**
   * Gets file uuid.
   *
   * @return the file uuid
   */
  public String getFileUuid() {
    return fileUuid;
  }

  /**
   * Sets file uuid.
   *
   * @param fileUuid the file uuid
   */
  public void setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
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
   * Gets size.
   *
   * @return the size
   */
  public long getSize() {
    return size;
  }

  /**
   * Sets size.
   *
   * @param size the size
   */
  public void setSize(long size) {
    this.size = size;
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

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(fileUuid, name, mimeType, size, checksumType, checksum);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
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
    final BaseFile other = (BaseFile) obj;
    return Objects.equals(this.fileUuid, other.fileUuid) && Objects.equals(this.name, other.name)
        && Objects.equals(this.mimeType, other.mimeType) && Objects.equals(this.size, other.size)
        && Objects.equals(this.checksumType, other.checksumType) && Objects.equals(this.checksum, other.checksum);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileUuid", fileUuid)
        .add("name", name)
        .add("mimeType", mimeType)
        .add("size", size)
        .add("checksumType", checksumType)
        .add("checksum", checksum)
        .toString();
  }
}
