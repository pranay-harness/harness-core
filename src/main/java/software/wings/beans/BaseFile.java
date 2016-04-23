package software.wings.beans;

import static software.wings.beans.ChecksumType.MD5;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Created by anubhaw on 4/13/16.
 */
public class BaseFile extends Base {
  private String fileUuid;
  private String name;
  private String mimeType;
  private long size;
  private ChecksumType checksumType;
  private String checksum;

  public BaseFile() {}

  public BaseFile(String fileName, String md5) {
    this.name = fileName;
    if (StringUtils.isNotBlank(md5)) {
      this.checksum = md5;
      this.checksumType = MD5;
    }
  }

  public String getFileUuid() {
    return fileUuid;
  }

  public void setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(fileUuid, name, mimeType, size, checksumType, checksum);
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
    final BaseFile other = (BaseFile) obj;
    return Objects.equals(this.fileUuid, other.fileUuid) && Objects.equals(this.name, other.name)
        && Objects.equals(this.mimeType, other.mimeType) && Objects.equals(this.size, other.size)
        && Objects.equals(this.checksumType, other.checksumType) && Objects.equals(this.checksum, other.checksum);
  }

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
