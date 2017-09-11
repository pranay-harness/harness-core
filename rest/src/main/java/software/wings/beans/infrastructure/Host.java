package software.wings.beans.infrastructure;

import com.amazonaws.services.ec2.model.Instance;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import javax.validation.constraints.NotNull;

/**
 * The Class Host.
 */
@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("serviceTemplateId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
@Data
@EqualsAndHashCode(callSuper = true)
public class Host extends Base {
  @NotEmpty private String envId;
  private String serviceTemplateId;
  private String infraMappingId;
  private String computeProviderId;
  @NotEmpty private String hostName;
  private String publicDns;
  @NotNull private String hostConnAttr;
  private String bastionConnAttr;

  private Instance ec2Instance;

  public String getPublicDns() {
    if (publicDns == null) {
      return hostName;
    }
    return publicDns;
  }

  public void setPublicDns(String publicDns) {
    this.publicDns = publicDns;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId;
    private String serviceTemplateId;
    private String infraMappingId;
    private String computeProviderId;
    private String hostName;
    private String publicDns;
    private String hostConnAttr;
    private String bastionConnAttr;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private Instance ec2Instance;

    private Builder() {}

    /**
     * A host builder.
     *
     * @return the builder
     */
    public static Builder aHost() {
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
     * With publicDns name builder.
     *
     * @param publicDns the host name
     * @return the builder
     */
    public Builder withPublicDns(String publicDns) {
      this.publicDns = publicDns;
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

    public Builder withEc2Instance(Instance ec2Instance) {
      this.ec2Instance = ec2Instance;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHost()
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
          .withLastUpdatedAt(lastUpdatedAt)
          .withPublicDns(publicDns)
          .withEc2Instance(ec2Instance);
    }

    /**
     * Build host.
     *
     * @return the host
     */
    public Host build() {
      Host host = new Host();
      host.setEnvId(envId);
      host.setServiceTemplateId(serviceTemplateId);
      host.setInfraMappingId(infraMappingId);
      host.setComputeProviderId(computeProviderId);
      host.setHostName(hostName);
      host.setPublicDns(publicDns);
      host.setHostConnAttr(hostConnAttr);
      host.setBastionConnAttr(bastionConnAttr);
      host.setUuid(uuid);
      host.setAppId(appId);
      host.setCreatedBy(createdBy);
      host.setCreatedAt(createdAt);
      host.setLastUpdatedBy(lastUpdatedBy);
      host.setLastUpdatedAt(lastUpdatedAt);
      host.setEc2Instance(ec2Instance);
      return host;
    }
  }
}
