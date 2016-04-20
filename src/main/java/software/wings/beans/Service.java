package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.ArtifactSource.ArtifactType;

import java.util.ArrayList;
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

  /**
   * Adds a platform software to service.
   * @param platformSoftware PlatformSoftware to add.
   */
  public void addPlatformSoftware(PlatformSoftware platformSoftware) {
    if (platformSoftwares == null) {
      platformSoftwares = new ArrayList<>();
    }
    platformSoftwares.add(platformSoftware);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name, description, artifactType, platformSoftwares);
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
    Service service = (Service) obj;
    return Objects.equals(name, service.name) && Objects.equals(description, service.description)
        && artifactType == service.artifactType && Objects.equals(platformSoftwares, service.platformSoftwares);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("artifactType", artifactType)
        .add("platformSoftwares", platformSoftwares)
        .toString();
  }

  public static class Builder {
    private String name;
    private String description;
    private ArtifactType artifactType;
    private List<PlatformSoftware> platformSoftwares;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * @return copy of Builder object.
     */
    public Builder but() {
      return aService()
          .withName(name)
          .withDescription(description)
          .withArtifactType(artifactType)
          .withPlatformSoftwares(platformSoftwares)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withPlatformSoftwares(List<PlatformSoftware> platformSoftwares) {
      this.platformSoftwares = platformSoftwares;
      return this;
    }

    public Builder withArtifactType(ArtifactType artifactType) {
      this.artifactType = artifactType;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public static Builder aService() {
      return new Builder();
    }

    /**
     * @return new Service object.
     */
    public Service build() {
      Service service = new Service();
      service.setName(name);
      service.setDescription(description);
      service.setArtifactType(artifactType);
      service.setPlatformSoftwares(platformSoftwares);
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
