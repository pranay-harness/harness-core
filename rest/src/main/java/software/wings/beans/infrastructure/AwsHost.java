package software.wings.beans.infrastructure;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ServiceTemplate;

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
    private String uuid;
    private String appId;
    private String infraId;
    private String hostName;
    private EmbeddedUser createdBy;
    private String osType;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private String hostConnAttr;
    private long lastUpdatedAt;
    private String bastionConnAttr;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private List<String> hostNames; // to support bulk add host API
    private List<ServiceTemplate> serviceTemplates; // to support bulk add host API

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
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With host conn attr builder.
     *
     * @param hostConnAttr the host conn attr
     * @return the builder
     */
    public Builder withHostConnAttr(String hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
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
     * With bastion conn attr builder.
     *
     * @param bastionConnAttr the bastion conn attr
     * @return the builder
     */
    public Builder withBastionConnAttr(String bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsHost()
          .withInstance(instance)
          .withUuid(uuid)
          .withAppId(appId)
          .withInfraId(infraId)
          .withHostName(hostName)
          .withCreatedBy(createdBy)
          .withOsType(osType)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withHostConnAttr(hostConnAttr)
          .withLastUpdatedAt(lastUpdatedAt)
          .withBastionConnAttr(bastionConnAttr)
          .withConfigFiles(configFiles)
          .withHostNames(hostNames)
          .withServiceTemplates(serviceTemplates);
    }

    /**
     * Build aws host.
     *
     * @return the aws host
     */
    public AwsHost build() {
      AwsHost awsHost = new AwsHost();
      awsHost.setInstance(instance);
      awsHost.setUuid(uuid);
      awsHost.setAppId(appId);
      awsHost.setInfraId(infraId);
      awsHost.setHostName(hostName);
      awsHost.setCreatedBy(createdBy);
      awsHost.setOsType(osType);
      awsHost.setCreatedAt(createdAt);
      awsHost.setLastUpdatedBy(lastUpdatedBy);
      awsHost.setHostConnAttr(hostConnAttr);
      awsHost.setLastUpdatedAt(lastUpdatedAt);
      awsHost.setBastionConnAttr(bastionConnAttr);
      awsHost.setConfigFiles(configFiles);
      awsHost.setHostNames(hostNames);
      awsHost.setServiceTemplates(serviceTemplates);
      return awsHost;
    }
  }
}
