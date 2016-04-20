package software.wings.beans;

public class FileMetadata {
  private String fileUuid;
  private String fileName;
  private String mimeType;
  private String fileDataType;
  private String fileRefId;
  private ChecksumType checksumType;
  private String checksum;

  public String getFileUuid() {
    return fileUuid;
  }

  public void setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
  }

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

  public String getFileDataType() {
    return fileDataType;
  }

  public void setFileDataType(String fileDataType) {
    this.fileDataType = fileDataType;
  }

  public String getFileRefId() {
    return fileRefId;
  }

  public void setFileRefId(String fileRefId) {
    this.fileRefId = fileRefId;
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
}
