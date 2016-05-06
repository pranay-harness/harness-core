package software.wings.beans;

import java.util.List;
import java.util.Set;

public class FileUploadSource extends ArtifactSource {
  private Set<String> serviceIds;

  public FileUploadSource() {
    super(SourceType.FILE_UPLOAD);
  }

  @Override
  public Set<String> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(Set<String> serviceIds) {
    this.serviceIds = serviceIds;
  }

  @Override
  public ArtifactFile collect(Object[] params) {
    return null;
  }

  public static final class Builder {
    private Set<String> serviceIds;
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    public static Builder aFileUploadSource() {
      return new Builder();
    }

    public Builder withServiceIds(Set<String> serviceIds) {
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
     * @return A new FileUploadSource object with given fields.
     */
    public FileUploadSource build() {
      FileUploadSource fileUploadSource = new FileUploadSource();
      fileUploadSource.setServiceIds(serviceIds);
      fileUploadSource.setSourceName(sourceName);
      fileUploadSource.setArtifactType(artifactType);
      return fileUploadSource;
    }
  }
}
