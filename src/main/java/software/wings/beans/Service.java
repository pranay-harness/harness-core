package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ArtifactSource.ArtifactType;

import java.util.List;
import java.util.Objects;

/**
 * Component bean class.
 *
 * @author Rishi
 */
@Entity(value = "services", noClassnameStored = true)
public class Service extends Base {
  private String name;
  private String description;
  private ArtifactType artifactType;

  @Reference(idOnly = true, ignoreMissing = true) private List<PlatformSoftware> platformSoftwares;

  @Transient private List<ConfigFile> configFiles;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ArtifactType getArtifactType() {
    return artifactType;
  }

  public void setArtifactType(ArtifactType artifactType) {
    this.artifactType = artifactType;
  }

  public List<PlatformSoftware> getPlatformSoftwares() {
    return platformSoftwares;
  }

  public void setPlatformSoftwares(List<PlatformSoftware> platformSoftwares) {
    this.platformSoftwares = platformSoftwares;
  }

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, artifactType, platformSoftwares, configFiles);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Service other = (Service) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.artifactType, other.artifactType)
        && Objects.equals(this.platformSoftwares, other.platformSoftwares)
        && Objects.equals(this.configFiles, other.configFiles);
  }

  public static final class ServiceBuilder {
    private String name;
    private String description;
    private ArtifactType artifactType;
    private List<PlatformSoftware> platformSoftwares;
    private List<ConfigFile> configFiles;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ServiceBuilder() {}

    public static ServiceBuilder aService() {
      return new ServiceBuilder();
    }

    public ServiceBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ServiceBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ServiceBuilder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public ServiceBuilder withPlatformSoftwares(List<PlatformSoftware> platformSoftwares) {
      this.platformSoftwares = platformSoftwares;
      return this;
    }

    public ServiceBuilder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    public ServiceBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ServiceBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ServiceBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ServiceBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ServiceBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ServiceBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ServiceBuilder but() {
      return aService()
          .withName(name)
          .withDescription(description)
          .withArtifactType(artifactType)
          .withPlatformSoftwares(platformSoftwares)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Service build() {
      Service service = new Service();
      service.setName(name);
      service.setDescription(description);
      service.setArtifactType(artifactType);
      service.setPlatformSoftwares(platformSoftwares);
      service.setConfigFiles(configFiles);
      service.setUuid(uuid);
      service.setCreatedBy(createdBy);
      service.setCreatedAt(createdAt);
      service.setLastUpdatedBy(lastUpdatedBy);
      service.setLastUpdatedAt(lastUpdatedAt);
      service.setActive(active);
      return service;
    }
  }
}
