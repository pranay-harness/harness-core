package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Environment.EnvironmentType;

import java.util.Objects;
import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "activities", noClassnameStored = true)
public class Activity extends Base {
  @NotEmpty private String environmentId;
  @NotEmpty private String environmentName;
  @NotNull private EnvironmentType environmentType;
  @NotEmpty private String commandName;
  @NotEmpty private String commandType;
  @NotEmpty private String serviceId;
  @NotEmpty private String serviceName;
  @NotEmpty private String serviceTemplateId;
  @NotEmpty private String serviceTemplateName;
  @NotEmpty private String hostName;
  private String releaseId;
  private String releaseName;
  private String artifactName;
  private Status status = Status.RUNNING;

  /**
   * Gets environment id.
   *
   * @return the environment id
   */
  public String getEnvironmentId() {
    return environmentId;
  }

  /**
   * Sets environment id.
   *
   * @param environmentId the environment id
   */
  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets command type.
   *
   * @return the command type
   */
  public String getCommandType() {
    return commandType;
  }

  /**
   * Sets command type.
   *
   * @param commandType the command type
   */
  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets service name.
   *
   * @return the service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets service name.
   *
   * @param serviceName the service name
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Gets service template id.
   *
   * @return the service template id
   */
  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  /**
   * Sets service template id.
   *
   * @param serviceTemplateId the service template id
   */
  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  /**
   * Gets service template name.
   *
   * @return the service template name
   */
  public String getServiceTemplateName() {
    return serviceTemplateName;
  }

  /**
   * Sets service template name.
   *
   * @param serviceTemplateName the service template name
   */
  public void setServiceTemplateName(String serviceTemplateName) {
    this.serviceTemplateName = serviceTemplateName;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets release id.
   *
   * @return the release id
   */
  public String getReleaseId() {
    return releaseId;
  }

  /**
   * Sets release id.
   *
   * @param releaseId the release id
   */
  public void setReleaseId(String releaseId) {
    this.releaseId = releaseId;
  }

  /**
   * Gets release name.
   *
   * @return the release name
   */
  public String getReleaseName() {
    return releaseName;
  }

  /**
   * Sets release name.
   *
   * @param releaseName the release name
   */
  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  /**
   * Gets artifact name.
   *
   * @return the artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Sets artifact name.
   *
   * @param artifactName the artifact name
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  public String getEnvironmentName() {
    return environmentName;
  }

  public void setEnvironmentName(String environmentName) {
    this.environmentName = environmentName;
  }

  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(environmentId, environmentName, environmentType, commandName, commandType, serviceId,
              serviceName, serviceTemplateId, serviceTemplateName, hostName, releaseId, releaseName, artifactName,
              status);
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
    final Activity other = (Activity) obj;
    return Objects.equals(this.environmentId, other.environmentId)
        && Objects.equals(this.environmentName, other.environmentName)
        && Objects.equals(this.environmentType, other.environmentType)
        && Objects.equals(this.commandName, other.commandName) && Objects.equals(this.commandType, other.commandType)
        && Objects.equals(this.serviceId, other.serviceId) && Objects.equals(this.serviceName, other.serviceName)
        && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.serviceTemplateName, other.serviceTemplateName)
        && Objects.equals(this.hostName, other.hostName) && Objects.equals(this.releaseId, other.releaseId)
        && Objects.equals(this.releaseName, other.releaseName) && Objects.equals(this.artifactName, other.artifactName)
        && Objects.equals(this.status, other.status);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("environmentId", environmentId)
        .add("environmentName", environmentName)
        .add("environmentType", environmentType)
        .add("commandName", commandName)
        .add("commandType", commandType)
        .add("serviceId", serviceId)
        .add("serviceName", serviceName)
        .add("serviceTemplateId", serviceTemplateId)
        .add("serviceTemplateName", serviceTemplateName)
        .add("hostName", hostName)
        .add("releaseId", releaseId)
        .add("releaseName", releaseName)
        .add("artifactName", artifactName)
        .add("status", status)
        .toString();
  }

  /**
   * The Enum Status.
   */
  public enum Status {
    /**
     * Running status.
     */
    RUNNING, /**
              * Completed status.
              */
    COMPLETED, /**
                * Aborted status.
                */
    ABORTED, /**
              * Failed status.
              */
    FAILED
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String environmentId;
    private String environmentName;
    private EnvironmentType environmentType;
    private String commandName;
    private String commandType;
    private String serviceId;
    private String serviceName;
    private String serviceTemplateId;
    private String serviceTemplateName;
    private String hostName;
    private String releaseId;
    private String releaseName;
    private String artifactName;
    private Status status = Status.RUNNING;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anActivity() {
      return new Builder();
    }

    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    public Builder withEnvironmentName(String environmentName) {
      this.environmentName = environmentName;
      return this;
    }

    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    public Builder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public Builder withCommandType(String commandType) {
      this.commandType = commandType;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceTemplateName(String serviceTemplateName) {
      this.serviceTemplateName = serviceTemplateName;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withReleaseId(String releaseId) {
      this.releaseId = releaseId;
      return this;
    }

    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return anActivity()
          .withEnvironmentId(environmentId)
          .withEnvironmentName(environmentName)
          .withEnvironmentType(environmentType)
          .withCommandName(commandName)
          .withCommandType(commandType)
          .withServiceId(serviceId)
          .withServiceName(serviceName)
          .withServiceTemplateId(serviceTemplateId)
          .withServiceTemplateName(serviceTemplateName)
          .withHostName(hostName)
          .withReleaseId(releaseId)
          .withReleaseName(releaseName)
          .withArtifactName(artifactName)
          .withStatus(status)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Activity build() {
      Activity activity = new Activity();
      activity.setEnvironmentId(environmentId);
      activity.setEnvironmentName(environmentName);
      activity.setEnvironmentType(environmentType);
      activity.setCommandName(commandName);
      activity.setCommandType(commandType);
      activity.setServiceId(serviceId);
      activity.setServiceName(serviceName);
      activity.setServiceTemplateId(serviceTemplateId);
      activity.setServiceTemplateName(serviceTemplateName);
      activity.setHostName(hostName);
      activity.setReleaseId(releaseId);
      activity.setReleaseName(releaseName);
      activity.setArtifactName(artifactName);
      activity.setStatus(status);
      activity.setUuid(uuid);
      activity.setAppId(appId);
      activity.setCreatedBy(createdBy);
      activity.setCreatedAt(createdAt);
      activity.setLastUpdatedBy(lastUpdatedBy);
      activity.setLastUpdatedAt(lastUpdatedAt);
      activity.setActive(active);
      return activity;
    }
  }
}
