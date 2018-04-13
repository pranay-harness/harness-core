package software.wings.beans.infrastructure;

import com.google.common.base.MoreObjects;

import com.amazonaws.services.ec2.model.Instance;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Objects;

/**
 * The Class Host.
 */
@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Host extends Base {
  @Indexed @NotEmpty private String envId;
  @Indexed private String serviceTemplateId;
  private String infraMappingId;
  private String computeProviderId;
  @Indexed @NotEmpty private String hostName;
  // In the case of EC2, publicDns could be either the public or private DNS name, depending on the setting in AWS_SSH
  // infrastructure mapping.
  @Indexed private String publicDns;
  private String hostConnAttr;
  private String bastionConnAttr;
  private String winrmConnAttr;

  private Instance ec2Instance;

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
   * Gets infra mapping id.
   *
   * @return the infra mapping id
   */
  public String getInfraMappingId() {
    return infraMappingId;
  }

  /**
   * Sets infra mapping id.
   *
   * @param infraMappingId the infra mapping id
   */
  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  /**
   * Gets compute provider id.
   *
   * @return the compute provider id
   */
  public String getComputeProviderId() {
    return computeProviderId;
  }

  /**
   * Sets compute provider id.
   *
   * @param computeProviderId the compute provider id
   */
  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
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
   * In the case of EC2, this could be either the public or private DNS name, depending on the setting in AWS_SSH
   * infrastructure mapping.
   *
   * @return the DNS name of the host.
   */
  public String getPublicDns() {
    if (publicDns == null) {
      return hostName;
    }
    return publicDns;
  }

  /**
   * In the case of EC2, this could be either the public or private DNS name, depending on the setting in AWS_SSH
   * infrastructure mapping.
   */
  public void setPublicDns(String publicDns) {
    this.publicDns = publicDns;
  }

  /**
   * Gets host conn attr.
   *
   * @return the host conn attr
   */
  public String getHostConnAttr() {
    return hostConnAttr;
  }

  /**
   * Sets host conn attr.
   *
   * @param hostConnAttr the host conn attr
   */
  public void setHostConnAttr(String hostConnAttr) {
    this.hostConnAttr = hostConnAttr;
  }

  /**
   * Gets bastion conn attr.
   *
   * @return the bastion conn attr
   */
  public String getBastionConnAttr() {
    return bastionConnAttr;
  }

  /**
   * Sets bastion conn attr.
   *
   * @param bastionConnAttr the bastion conn attr
   */
  public void setBastionConnAttr(String bastionConnAttr) {
    this.bastionConnAttr = bastionConnAttr;
  }

  public String getWinrmConnAttr() {
    return winrmConnAttr;
  }

  public void setWinrmConnAttr(String winrmConnAttr) {
    this.winrmConnAttr = winrmConnAttr;
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

  public Instance getEc2Instance() {
    return ec2Instance;
  }

  public void setEc2Instance(Instance ec2Instance) {
    this.ec2Instance = ec2Instance;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(envId, serviceTemplateId, infraMappingId, computeProviderId, hostName, hostConnAttr,
              bastionConnAttr, winrmConnAttr);
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
    final Host other = (Host) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.infraMappingId, other.infraMappingId)
        && Objects.equals(this.computeProviderId, other.computeProviderId)
        && Objects.equals(this.hostName, other.hostName) && Objects.equals(this.hostConnAttr, other.hostConnAttr)
        && Objects.equals(this.bastionConnAttr, other.bastionConnAttr)
        && Objects.equals(this.winrmConnAttr, other.winrmConnAttr);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("serviceTemplateId", serviceTemplateId)
        .add("infraMappingId", infraMappingId)
        .add("computeProviderId", computeProviderId)
        .add("hostName", hostName)
        .add("publicDns", publicDns)
        .add("hostConnAttr", hostConnAttr)
        .add("bastionConnAttr", bastionConnAttr)
        .add("winrmConnAttr", winrmConnAttr)
        .toString();
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
    private String winrmConnAttr;
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
     * With winrm conn attr builder.
     *
     * @param winrmConnAttr the winrm conn attr
     * @return the builder
     */
    public Builder withWinrmConnAttr(String winrmConnAttr) {
      this.winrmConnAttr = winrmConnAttr;
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
          .withWinrmConnAttr(winrmConnAttr)
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
      host.setWinrmConnAttr(winrmConnAttr);
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
