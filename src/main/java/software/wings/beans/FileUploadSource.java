package software.wings.beans;

import com.google.common.collect.Sets;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.mongodb.morphia.annotations.Reference;
import software.wings.utils.ArtifactType;

import java.util.Set;

/**
 * The Class FileUploadSource.
 */
@JsonTypeName("FILE_UPLOAD")
public class FileUploadSource extends ArtifactSource {
  @Reference(idOnly = true, ignoreMissing = true, lazy = true) private Set<Service> services;

  /**
   * Instantiates a new file upload source.
   */
  public FileUploadSource() {
    super(SourceType.FILE_UPLOAD);
  }

  @Override
  public Set<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(Set<Service> services) {
    this.services = services;
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private Set<Service> services = Sets.newHashSet();
    private String sourceName;
    private ArtifactType artifactType;

    private Builder() {}

    /**
     * A file upload source.
     *
     * @return the builder
     */
    public static Builder aFileUploadSource() {
      return new Builder();
    }

    /**
     * With services.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(Set<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With source name.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With artifact type.
     *
     * @param artifactType the artifact type
     * @return the builder
     */
    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    /**
     * Builds the.
     *
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
