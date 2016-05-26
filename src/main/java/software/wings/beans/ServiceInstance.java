package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;

import java.util.Objects;

@Entity(value = "serviceInstance", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("host"), @Field("serviceTemplate") },
    options = @IndexOptions(unique = true)))
public class ServiceInstance extends Base {
  private String envId;
  @Reference(idOnly = true, ignoreMissing = true) private Host host;
  @Reference(idOnly = true, ignoreMissing = true) private Service service;
  @Reference(idOnly = true, ignoreMissing = true) private ServiceTemplate serviceTemplate;
  @Reference(idOnly = true, ignoreMissing = true) private Release release;
  @Reference(idOnly = true, ignoreMissing = true) private Artifact artifact;

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public Host getHost() {
    return host;
  }

  public void setHost(Host host) {
    this.host = host;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  public Release getRelease() {
    return release;
  }

  public void setRelease(Release release) {
    this.release = release;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, host, service, serviceTemplate, release, artifact);
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
    final ServiceInstance other = (ServiceInstance) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.host, other.host)
        && Objects.equals(this.service, other.service) && Objects.equals(this.serviceTemplate, other.serviceTemplate)
        && Objects.equals(this.release, other.release) && Objects.equals(this.artifact, other.artifact);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("host", host)
        .add("service", service)
        .add("serviceTemplate", serviceTemplate)
        .add("release", release)
        .add("artifact", artifact)
        .toString();
  }

  public static final class ServiceInstanceBuilder {
    private String envId;
    private Host host;
    private Service service;
    private ServiceTemplate serviceTemplate;
    private Release release;
    private Artifact artifact;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private ServiceInstanceBuilder() {}

    public static ServiceInstanceBuilder aServiceInstance() {
      return new ServiceInstanceBuilder();
    }

    public ServiceInstanceBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public ServiceInstanceBuilder withHost(Host host) {
      this.host = host;
      return this;
    }

    public ServiceInstanceBuilder withService(Service service) {
      this.service = service;
      return this;
    }

    public ServiceInstanceBuilder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceTemplate = serviceTemplate;
      return this;
    }

    public ServiceInstanceBuilder withRelease(Release release) {
      this.release = release;
      return this;
    }

    public ServiceInstanceBuilder withArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    public ServiceInstanceBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public ServiceInstanceBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ServiceInstanceBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public ServiceInstanceBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public ServiceInstanceBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public ServiceInstanceBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public ServiceInstanceBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ServiceInstanceBuilder but() {
      return aServiceInstance()
          .withEnvId(envId)
          .withHost(host)
          .withService(service)
          .withServiceTemplate(serviceTemplate)
          .withRelease(release)
          .withArtifact(artifact)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public ServiceInstance build() {
      ServiceInstance serviceInstance = new ServiceInstance();
      serviceInstance.setEnvId(envId);
      serviceInstance.setHost(host);
      serviceInstance.setService(service);
      serviceInstance.setServiceTemplate(serviceTemplate);
      serviceInstance.setRelease(release);
      serviceInstance.setArtifact(artifact);
      serviceInstance.setUuid(uuid);
      serviceInstance.setAppId(appId);
      serviceInstance.setCreatedBy(createdBy);
      serviceInstance.setCreatedAt(createdAt);
      serviceInstance.setLastUpdatedBy(lastUpdatedBy);
      serviceInstance.setLastUpdatedAt(lastUpdatedAt);
      serviceInstance.setActive(active);
      return serviceInstance;
    }
  }
}
