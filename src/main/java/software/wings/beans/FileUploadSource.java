package software.wings.beans;

public class FileUploadSource extends ArtifactSource {
  public FileUploadSource() {
    super(SourceType.FILE_UPLOAD);
  }

  @Override
  public ArtifactFile collect(Object[] params) {
    return null;
  }
}
