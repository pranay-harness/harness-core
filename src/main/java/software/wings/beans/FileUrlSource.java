package software.wings.beans;

/**
 * Created by anubhaw on 4/13/16.
 */
public class FileUrlSource extends ArtifactSource {
  private String url;

  public FileUrlSource() {
    super(SourceType.HTTP);
  }

  @Override
  public ArtifactFile collect(Object[] params) {
    return null;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
