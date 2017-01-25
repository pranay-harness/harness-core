package software.wings.beans.infrastructure;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.EmbeddedUser;

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
    private String envId;
    private String serviceTemplateId;
    private String infraMappingId;
    private String computeProviderId;
    private String hostName;
    private String hostConnAttr;
    private String bastionConnAttr;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

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
     * With service template id builder.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    /**
     * With infra mapping id builder.
     *
     * @param infraMappingId the infra mapping id
     * @return the builder
     */
    public Builder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    /**
     * With compute provider id builder.
     *
     * @param computeProviderId the compute provider id
     * @return the builder
     */
    public Builder withComputeProviderId(String computeProviderId) {
      this.computeProviderId = computeProviderId;
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
    public Builder withCreatedBy(EmbeddedUser createdBy) {
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
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsHost()
          .withInstance(instance)
          .withEnvId(envId)
          .withServiceTemplateId(serviceTemplateId)
          .withInfraMappingId(infraMappingId)
          .withComputeProviderId(computeProviderId)
          .withHostName(hostName)
          .withHostConnAttr(hostConnAttr)
          .withBastionConnAttr(bastionConnAttr)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build aws host.
     *
     * @return the aws host
     */
    public AwsHost build() {
      AwsHost awsHost = new AwsHost();
      awsHost.setInstance(instance);
      awsHost.setEnvId(envId);
      awsHost.setServiceTemplateId(serviceTemplateId);
      awsHost.setInfraMappingId(infraMappingId);
      awsHost.setComputeProviderId(computeProviderId);
      awsHost.setHostName(hostName);
      awsHost.setHostConnAttr(hostConnAttr);
      awsHost.setBastionConnAttr(bastionConnAttr);
      awsHost.setUuid(uuid);
      awsHost.setAppId(appId);
      awsHost.setCreatedBy(createdBy);
      awsHost.setCreatedAt(createdAt);
      awsHost.setLastUpdatedBy(lastUpdatedBy);
      awsHost.setLastUpdatedAt(lastUpdatedAt);
      return awsHost;
    }
  }
}
