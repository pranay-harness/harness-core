package software.wings.beans;

public class ArtifactFile {
  private String fileUuid;
  private String fileName;
  private String mimeType;

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
}
