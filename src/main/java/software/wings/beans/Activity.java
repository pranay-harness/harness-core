package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "activities", noClassnameStored = true)
public class Activity extends Base {
  @NotEmpty private String environmentId;
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

  public String getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getCommandType() {
    return commandType;
  }

  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  public String getServiceTemplateName() {
    return serviceTemplateName;
  }

  public void setServiceTemplateName(String serviceTemplateName) {
    this.serviceTemplateName = serviceTemplateName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getReleaseId() {
    return releaseId;
  }

  public void setReleaseId(String releaseId) {
    this.releaseId = releaseId;
  }

  public String getReleaseName() {
    return releaseName;
  }

  public void setReleaseName(String releaseName) {
    this.releaseName = releaseName;
  }

  public String getArtifactName() {
    return artifactName;
  }

  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("environmentId", environmentId)
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

  public enum Status { RUNNING, COMPLETED, ABORTED, FAILED }

  public static final class Builder {
    private String environmentId;
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
