/**
 *
 */

package software.wings.api;

import com.amazonaws.services.ec2.model.Instance;
import com.google.common.base.MoreObjects;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

import static software.wings.api.HostElement.Builder.aHostElement;

/**
 * The Class HostElement.
 *
 * @author Rishi
 */
public class HostElement implements ContextElement {
  private String uuid;
  private String hostName;
  private String instanceId;
  private String publicDns;
  private Instance ec2Instance;

  @Override
  public String getName() {
    return hostName;
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

  public String getPublicDns() {
    return publicDns;
  }

  public void setPublicDns(String publicDns) {
    this.publicDns = publicDns;
  }

  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Instance getEc2Instance() {
    return ec2Instance;
  }

  public void setEc2Instance(Instance ec2Instance) {
    this.ec2Instance = ec2Instance;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HOST;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(ContextElement.HOST, this);
    return map;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("uuid", uuid)
        .add("hostName", hostName)
        .add("instanceId", instanceId)
        .toString();
  }

  /**
   * Gets instance id.
   *
   * @return the instance id
   */
  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Sets instance id.
   *
   * @param instanceId the instance id
   */
  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public ContextElement cloneMin() {
    return aHostElement()
        .withUuid(uuid)
        .withHostName(hostName)
        .withPublicDns(publicDns)
        .withInstanceId(instanceId)
        .withEc2Instance(ec2Instance)
        .build();
  }

  public static final class Builder {
    private String uuid;
    private String hostName;
    private String instanceId;
    private String publicDns;
    private Instance ec2Instance;

    private Builder() {}

    public static Builder aHostElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withInstanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder withPublicDns(String publicDns) {
      this.publicDns = publicDns;
      return this;
    }

    public Builder withEc2Instance(Instance ec2Instance) {
      this.ec2Instance = ec2Instance;
      return this;
    }

    public HostElement build() {
      HostElement hostElement = new HostElement();
      hostElement.setUuid(uuid);
      hostElement.setHostName(hostName);
      hostElement.setInstanceId(instanceId);
      hostElement.setPublicDns(publicDns);
      hostElement.setEc2Instance(ec2Instance);
      return hostElement;
    }
  }
}
