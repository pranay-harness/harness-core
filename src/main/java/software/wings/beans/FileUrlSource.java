package software.wings.beans;

import org.mongodb.morphia.annotations.Reference;

import java.util.Set;

/**
 * Created by anubhaw on 4/13/16.
 */
public class FileUrlSource extends ArtifactSource {
  private String url;

  @Reference(idOnly = true, ignoreMissing = true, lazy = true) private Set<Service> services;

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

  @Override
  public Set<Service> getServices() {
    return services;
  }

  public void setServices(Set<Service> services) {
    this.services = services;
  }

  public static final class Builder {
    private String url;
    private Set<Service> services;
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

    public Builder withServices(Set<Service> serviceIds) {
      this.services = serviceIds;
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
      fileUrlSource.setServices(services);
      fileUrlSource.setSourceName(sourceName);
      fileUrlSource.setArtifactType(artifactType);
      return fileUrlSource;
    }
  }
}
