package software.wings.beans;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 4/13/16.
 */
public class FileUrlSource extends ArtifactSource {
  private String url;

  private Set<String> serviceIds;

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

  public Set<String> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(Set<String> serviceIds) {
    this.serviceIds = serviceIds;
  }

  public static final class Builder {
    private String url;
    private Set<String> serviceIds;
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    public static Builder aFileUrlSource() {
      return new Builder();
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder withServices(Set<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * @return a new FileUrlSource object with given fields.
     */
    public FileUrlSource build() {
      FileUrlSource fileUrlSource = new FileUrlSource();
      fileUrlSource.setUrl(url);
      fileUrlSource.setServiceIds(serviceIds);
      fileUrlSource.setSourceName(sourceName);
      fileUrlSource.setArtifactType(artifactType);
      return fileUrlSource;
    }
  }
}
