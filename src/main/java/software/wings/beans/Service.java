package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ArtifactSource.ArtifactType;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.List;
import java.util.Map;

/**
 * Component bean class.
 *
 * @author Rishi
 */
@Entity(value = "services", noClassnameStored = true)
public class Service extends Base implements ContextElement {
  private static final long serialVersionUID = -5785133514617556212L;

  private String name;
  private String description;
  private ArtifactType artifactType;

  @Reference(idOnly = true, ignoreMissing = true) private AppContainer appContainer;

  @Transient private List<ConfigFile> configFiles;

  @Override
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

  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  public AppContainer getAppContainer() {
    return appContainer;
  }

  public void setAppContainer(AppContainer appContainer) {
    this.appContainer = appContainer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    Service service = (Service) o;
    return com.google.common.base.Objects.equal(name, service.name)
        && com.google.common.base.Objects.equal(description, service.description)
        && artifactType == service.artifactType
        && com.google.common.base.Objects.equal(appContainer, service.appContainer)
        && com.google.common.base.Objects.equal(configFiles, service.configFiles);
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(
        super.hashCode(), name, description, artifactType, appContainer, configFiles);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("artifactType", artifactType)
        .add("configFiles", configFiles)
        .add("repeatElementType", getElementType())
        .toString();
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SERVICE;
  }

  public static final class ServiceBuilder {
    private String name;
    private String description;
    private ArtifactType artifactType;
    private AppContainer appContainer;
    private List<ConfigFile> configFiles;
    private String uuid;
    private String appId;
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

    public ServiceBuilder withAppContainer(AppContainer appContainer) {
      this.appContainer = appContainer;
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

    public ServiceBuilder withAppId(String appId) {
      this.appId = appId;
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
          .withAppContainer(appContainer)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
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
      service.setAppContainer(appContainer);
      service.setConfigFiles(configFiles);
      service.setUuid(uuid);
      service.setAppId(appId);
      service.setCreatedBy(createdBy);
      service.setCreatedAt(createdAt);
      service.setLastUpdatedBy(lastUpdatedBy);
      service.setLastUpdatedAt(lastUpdatedAt);
      service.setActive(active);
      return service;
    }
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }
}
