package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * The Class ServiceInstance.
 */
@Entity(value = "serviceInstance", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("host"), @Field("serviceTemplate") },
    options = @IndexOptions(unique = true)))
public class ServiceInstance extends Base {
  private String envId;
  @Reference(idOnly = true, ignoreMissing = true) private Host host;
  @Reference(idOnly = true, ignoreMissing = true) private ServiceTemplate serviceTemplate;
  @Reference(idOnly = true, ignoreMissing = true) private Release release;
  @Reference(idOnly = true, ignoreMissing = true) private Artifact artifact;

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets host.
   *
   * @return the host
   */
  public Host getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(Host host) {
    this.host = host;
  }

  /**
   * Gets service template.
   *
   * @return the service template
   */
  public ServiceTemplate getServiceTemplate() {
    return serviceTemplate;
  }

  /**
   * Sets service template.
   *
   * @param serviceTemplate the service template
   */
  public void setServiceTemplate(ServiceTemplate serviceTemplate) {
    this.serviceTemplate = serviceTemplate;
  }

  /**
   * Gets release.
   *
   * @return the release
   */
  public Release getRelease() {
    return release;
  }

  /**
   * Sets release.
   *
   * @param release the release
   */
  public void setRelease(Release release) {
    this.release = release;
  }

  /**
   * Gets artifact.
   *
   * @return the artifact
   */
  public Artifact getArtifact() {
    return artifact;
  }

  /**
   * Sets artifact.
   *
   * @param artifact the artifact
   */
  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return host.getHostName() + ":" + serviceTemplate.getName();
  }

  /**
   * Sets display name.
   */
  public void setDisplayName() {}

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, host, serviceTemplate, release, artifact);
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
        && Objects.equals(this.serviceTemplate, other.serviceTemplate) && Objects.equals(this.release, other.release)
        && Objects.equals(this.artifact, other.artifact);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("host", host)
        .add("serviceTemplate", serviceTemplate)
        .add("release", release)
        .add("artifact", artifact)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String envId;
    private Host host;
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

    private Builder() {}

    /**
     * A service instance service instance builder.
     *
     * @return the service instance builder
     */
    public static Builder aServiceInstance() {
      return new Builder();
    }

    /**
     * With env id service instance builder.
     *
     * @param envId the env id
     * @return the service instance builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With host service instance builder.
     *
     * @param host the host
     * @return the service instance builder
     */
    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    /**
     * With service template service instance builder.
     *
     * @param serviceTemplate the service template
     * @return the service instance builder
     */
    public Builder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceTemplate = serviceTemplate;
      return this;
    }

    /**
     * With release service instance builder.
     *
     * @param release the release
     * @return the service instance builder
     */
    public Builder withRelease(Release release) {
      this.release = release;
      return this;
    }

    /**
     * With artifact service instance builder.
     *
     * @param artifact the artifact
     * @return the service instance builder
     */
    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    /**
     * With uuid service instance builder.
     *
     * @param uuid the uuid
     * @return the service instance builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id service instance builder.
     *
     * @param appId the app id
     * @return the service instance builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by service instance builder.
     *
     * @param createdBy the created by
     * @return the service instance builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at service instance builder.
     *
     * @param createdAt the created at
     * @return the service instance builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by service instance builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the service instance builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at service instance builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the service instance builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active service instance builder.
     *
     * @param active the active
     * @return the service instance builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But service instance builder.
     *
     * @return the service instance builder
     */
    public Builder but() {
      return aServiceInstance()
          .withEnvId(envId)
          .withHost(host)
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

    /**
     * Build service instance.
     *
     * @return the service instance
     */
    public ServiceInstance build() {
      ServiceInstance serviceInstance = new ServiceInstance();
      serviceInstance.setEnvId(envId);
      serviceInstance.setHost(host);
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
