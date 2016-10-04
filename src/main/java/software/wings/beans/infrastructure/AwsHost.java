package software.wings.beans.infrastructure;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 10/4/16.
 */
public class AwsHost extends Host {
  private Instance instance;

  /**
   * Gets instance.
   *
   * @return the instance
   */
  public Instance getInstance() {
    return instance;
  }

  /**
   * Sets instance.
   *
   * @param instance the instance
   */
  public void setInstance(Instance instance) {
    this.instance = instance;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Instance instance;
    private String infraId;
    private String hostName;
    private String osType;
    private SettingAttribute hostConnAttr;
    private SettingAttribute bastionConnAttr;
    private Tag configTag;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private List<String> hostNames; // to support bulk add host API
    private List<ServiceTemplate> serviceTemplates; // to support bulk add host API
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An aws host builder.
     *
     * @return the builder
     */
    public static Builder anAwsHost() {
      return new Builder();
    }

    /**
     * With instance builder.
     *
     * @param instance the instance
     * @return the builder
     */
    public Builder withInstance(Instance instance) {
      this.instance = instance;
      return this;
    }

    /**
     * With infra id builder.
     *
     * @param infraId the infra id
     * @return the builder
     */
    public Builder withInfraId(String infraId) {
      this.infraId = infraId;
      return this;
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With os type builder.
     *
     * @param osType the os type
     * @return the builder
     */
    public Builder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    /**
     * With host conn attr builder.
     *
     * @param hostConnAttr the host conn attr
     * @return the builder
     */
    public Builder withHostConnAttr(SettingAttribute hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
      return this;
    }

    /**
     * With bastion conn attr builder.
     *
     * @param bastionConnAttr the bastion conn attr
     * @return the builder
     */
    public Builder withBastionConnAttr(SettingAttribute bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
      return this;
    }

    /**
     * With config tag builder.
     *
     * @param configTag the config tag
     * @return the builder
     */
    public Builder withConfigTag(Tag configTag) {
      this.configTag = configTag;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFiles the config files
     * @return the builder
     */
    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    /**
     * With host names builder.
     *
     * @param hostNames the host names
     * @return the builder
     */
    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    /**
     * With service templates builder.
     *
     * @param serviceTemplates the service templates
     * @return the builder
     */
    public Builder withServiceTemplates(List<ServiceTemplate> serviceTemplates) {
      this.serviceTemplates = serviceTemplates;
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
      return anAwsHost()
          .withInstance(instance)
          .withInfraId(infraId)
          .withHostName(hostName)
          .withOsType(osType)
          .withHostConnAttr(hostConnAttr)
          .withBastionConnAttr(bastionConnAttr)
          .withConfigTag(configTag)
          .withConfigFiles(configFiles)
          .withHostNames(hostNames)
          .withServiceTemplates(serviceTemplates)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build aws host.
     *
     * @return the aws host
     */
    public AwsHost build() {
      AwsHost awsHost = new AwsHost();
      awsHost.setInstance(instance);
      awsHost.setInfraId(infraId);
      awsHost.setHostName(hostName);
      awsHost.setOsType(osType);
      awsHost.setHostConnAttr(hostConnAttr);
      awsHost.setBastionConnAttr(bastionConnAttr);
      awsHost.setConfigTag(configTag);
      awsHost.setConfigFiles(configFiles);
      awsHost.setHostNames(hostNames);
      awsHost.setServiceTemplates(serviceTemplates);
      awsHost.setUuid(uuid);
      awsHost.setAppId(appId);
      awsHost.setCreatedBy(createdBy);
      awsHost.setCreatedAt(createdAt);
      awsHost.setLastUpdatedBy(lastUpdatedBy);
      awsHost.setLastUpdatedAt(lastUpdatedAt);
      awsHost.setActive(active);
      return awsHost;
    }
  }
}
