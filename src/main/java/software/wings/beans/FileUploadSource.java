package software.wings.beans;

import com.google.common.collect.Sets;

import org.mongodb.morphia.annotations.Reference;

import java.util.Set;

public class FileUploadSource extends ArtifactSource {
  @Reference(idOnly = true, ignoreMissing = true, lazy = true) private Set<Service> services;

  public FileUploadSource() {
    super(SourceType.FILE_UPLOAD);
  }

  @Override
  public Set<Service> getServices() {
    return services;
  }

  public void setServices(Set<Service> services) {
    this.services = services;
  }

  @Override
  public ArtifactFile collect(Object[] params) {
    return null;
  }

  public static final class Builder {
    private Set<Service> services = Sets.newHashSet();
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    public static Builder aFileUploadSource() {
      return new Builder();
    }

    public Builder withServices(Set<Service> services) {
      this.services = services;
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
     * @return A new FileUploadSource object with given fields.
     */
    public FileUploadSource build() {
      FileUploadSource fileUploadSource = new FileUploadSource();
      fileUploadSource.setServices(services);
      fileUploadSource.setSourceName(sourceName);
      fileUploadSource.setArtifactType(artifactType);
      return fileUploadSource;
    }
  }
}
