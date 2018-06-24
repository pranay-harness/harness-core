package software.wings.beans.command;

import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by peeyushaggarwal on 11/16/16.
 */
@Entity(value = "serviceCommands", noClassnameStored = true)
public class ServiceCommand extends Base {
  private String name;
  @Indexed private String serviceId;
  private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();
  private Integer defaultVersion;

  private boolean targetToAllEnv = true;

  @Transient private Command command;

  @Transient @JsonIgnore private boolean setAsDefault;

  @Transient private String notes;

  @JsonIgnore private double order;

  /**
   * Getter for property 'name'.
   *
   * @return Value for property 'name'.
   */
  public String getName() {
    return name;
  }

  /**
   * Setter for property 'name'.
   *
   * @param name Value to set for property 'name'.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Getter for property 'serviceId'.
   *
   * @return Value for property 'serviceId'.
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Setter for property 'serviceId'.
   *
   * @param serviceId Value to set for property 'serviceId'.
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Getter for property 'envIdVersionMap'.
   *
   * @return Value for property 'envIdVersionMap'.
   */
  public Map<String, EntityVersion> getEnvIdVersionMap() {
    return envIdVersionMap;
  }

  /**
   * Setter for property 'envIdVersionMap'.
   *
   * @param envIdVersionMap Value to set for property 'envIdVersionMap'.
   */
  public void setEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
    this.envIdVersionMap = envIdVersionMap;
  }

  /**
   * Getter for property 'defaultVersion'.
   *
   * @return Value for property 'defaultVersion'.
   */
  public Integer getDefaultVersion() {
    return defaultVersion;
  }

  /**
   * Setter for property 'defaultVersion'.
   *
   * @param defaultVersion Value to set for property 'defaultVersion'.
   */
  public void setDefaultVersion(Integer defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  /**
   * Getter for property 'command'.
   *
   * @return Value for property 'command'.
   */
  public Command getCommand() {
    return command;
  }

  /**
   * Setter for property 'command'.
   *
   * @param command Value to set for property 'command'.
   */
  public void setCommand(Command command) {
    this.command = command;
  }

  /**
   * Getter for property 'setAsDefault'.
   *
   * @return Value for property 'setAsDefault'.
   */
  @JsonIgnore
  public boolean getSetAsDefault() {
    return setAsDefault;
  }

  /**
   * Setter for property 'setAsDefault'.
   *
   * @param setAsDefault Value to set for property 'setAsDefault'.
   */
  @JsonProperty
  public void setSetAsDefault(boolean setAsDefault) {
    this.setAsDefault = setAsDefault;
  }

  /**
   * Getter for property 'notes'.
   *
   * @return Value for property 'notes'.
   */
  @JsonIgnore
  public String getNotes() {
    return notes;
  }

  /**
   * Setter for property 'notes'.
   *
   * @param notes Value to set for property 'notes'.
   */
  @JsonProperty
  public void setNotes(String notes) {
    this.notes = notes;
  }

  /**
   * Getter for property 'targetToAllEnv'.
   *
   * @return Value for property 'targetToAllEnv'.
   */
  public boolean isTargetToAllEnv() {
    return targetToAllEnv;
  }

  /**
   * Setter for property 'targetToAllEnv'.
   *
   * @param targetToAllEnv Value to set for property 'targetToAllEnv'.
   */
  public void setTargetToAllEnv(boolean targetToAllEnv) {
    this.targetToAllEnv = targetToAllEnv;
  }

  public double getOrder() {
    return order;
  }

  public void setOrder(double order) {
    this.order = order;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, serviceId, envIdVersionMap, defaultVersion, targetToAllEnv);
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
    final ServiceCommand other = (ServiceCommand) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.serviceId, other.serviceId)
        && Objects.equals(this.envIdVersionMap, other.envIdVersionMap)
        && Objects.equals(this.defaultVersion, other.defaultVersion)
        && Objects.equals(this.targetToAllEnv, other.targetToAllEnv);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("serviceId", serviceId)
        .add("envIdVersionMap", envIdVersionMap)
        .add("defaultVersion", defaultVersion)
        .add("targetToAllEnv", targetToAllEnv)
        .toString();
  }

  @JsonIgnore
  public int getVersionForEnv(String envId) {
    if (targetToAllEnv || envIdVersionMap.get(envId) != null) {
      return Optional.ofNullable(envIdVersionMap.get(envId))
          .orElse(anEntityVersion().withVersion(defaultVersion).build())
          .getVersion();
    } else {
      return 0;
    }
  }

  public ServiceCommand cloneInternal() {
    return aServiceCommand()
        .withName(getName())
        .withTargetToAllEnv(targetToAllEnv)
        .withCommand(getCommand().cloneInternal())
        .build();
  }

  public static final class Builder {
    private String name;
    private String serviceId;
    private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();
    private Integer defaultVersion;
    private boolean targetToAllEnv = true;
    private boolean setAsDefault;
    private Command command;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aServiceCommand() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withEnvIdVersionMap(Map<String, EntityVersion> envIdVersionMap) {
      this.envIdVersionMap = envIdVersionMap;
      return this;
    }

    public Builder withDefaultVersion(Integer defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    public Builder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    public Builder withSetAsDefault(boolean setAsDefault) {
      this.setAsDefault = setAsDefault;
      return this;
    }

    public Builder withCommand(Command command) {
      this.command = command;
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

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aServiceCommand()
          .withName(name)
          .withServiceId(serviceId)
          .withEnvIdVersionMap(envIdVersionMap)
          .withDefaultVersion(defaultVersion)
          .withTargetToAllEnv(targetToAllEnv)
          .withCommand(command)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withSetAsDefault(setAsDefault);
    }

    public ServiceCommand build() {
      ServiceCommand serviceCommand = new ServiceCommand();
      serviceCommand.setName(name);
      serviceCommand.setServiceId(serviceId);
      serviceCommand.setEnvIdVersionMap(envIdVersionMap);
      serviceCommand.setDefaultVersion(defaultVersion);
      serviceCommand.setCommand(command);
      serviceCommand.setUuid(uuid);
      serviceCommand.setAppId(appId);
      serviceCommand.setCreatedBy(createdBy);
      serviceCommand.setCreatedAt(createdAt);
      serviceCommand.setLastUpdatedBy(lastUpdatedBy);
      serviceCommand.setLastUpdatedAt(lastUpdatedAt);
      serviceCommand.setSetAsDefault(setAsDefault);
      serviceCommand.targetToAllEnv = this.targetToAllEnv;
      return serviceCommand;
    }
  }
}
