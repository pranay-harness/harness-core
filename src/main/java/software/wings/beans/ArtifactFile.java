package software.wings.beans;

public class ArtifactFile {
  private String fileUUID;
  private String fileName;
  private String mimeType;
  public String getFileUUID() {
    return fileUUID;
  }
  public void setFileUUID(String fileUUID) {
    this.fileUUID = fileUUID;
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
