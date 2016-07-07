package software.wings.beans;

import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;

import java.util.Objects;
import java.util.Optional;

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
  private long lastDeployedOn;

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
    return Optional.ofNullable(host).orElse(aHost().withHostName("").build()).getHostName() + ":"
        + Optional.ofNullable(serviceTemplate).orElse(aServiceTemplate().withName("").build()).getName();
  }

  /**
   * Sets display name.
   */
  public void setDisplayName() {}

  /**
   * Gets last deployed on.
   *
   * @return the last deployed on
   */
  public long getLastDeployedOn() {
    return lastDeployedOn;
  }

  /**
   * Sets last deployed on.
   *
   * @param lastDeployedOn the last deployed on
   */
  public void setLastDeployedOn(long lastDeployedOn) {
    this.lastDeployedOn = lastDeployedOn;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, host, serviceTemplate, release, artifact, lastDeployedOn);
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
        && Objects.equals(this.artifact, other.artifact) && Objects.equals(this.lastDeployedOn, other.lastDeployedOn);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("host", host)
        .add("serviceTemplate", serviceTemplate)
        .add("release", release)
        .add("artifact", artifact)
        .add("lastDeployedOn", lastDeployedOn)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private Host host;
    private ServiceTemplate serviceTemplate;
    private Release release;
    private Artifact artifact;
    private long lastDeployedOn;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A service instance builder.
     *
     * @return the builder
     */
    public static Builder aServiceInstance() {
      return new Builder();
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With host builder.
     *
     * @param host the host
     * @return the builder
     */
    public Builder withHost(Host host) {
      this.host = host;
      return this;
    }

    /**
     * With service template builder.
     *
     * @param serviceTemplate the service template
     * @return the builder
     */
    public Builder withServiceTemplate(ServiceTemplate serviceTemplate) {
      this.serviceTemplate = serviceTemplate;
      return this;
    }

    /**
     * With release builder.
     *
     * @param release the release
     * @return the builder
     */
    public Builder withRelease(Release release) {
      this.release = release;
      return this;
    }

    /**
     * With artifact builder.
     *
     * @param artifact the artifact
     * @return the builder
     */
    public Builder withArtifact(Artifact artifact) {
      this.artifact = artifact;
      return this;
    }

    /**
     * With last deployed on builder.
     *
     * @param lastDeployedOn the last deployed on
     * @return the builder
     */
    public Builder withLastDeployedOn(long lastDeployedOn) {
      this.lastDeployedOn = lastDeployedOn;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aServiceInstance()
          .withEnvId(envId)
          .withHost(host)
          .withServiceTemplate(serviceTemplate)
          .withRelease(release)
          .withArtifact(artifact)
          .withLastDeployedOn(lastDeployedOn)
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
      serviceInstance.setLastDeployedOn(lastDeployedOn);
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
