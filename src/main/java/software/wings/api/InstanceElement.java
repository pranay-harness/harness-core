/**
 *
 */

package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class InstanceElement.
 *
 * @author Rishi
 */
public class InstanceElement implements ContextElement {
  private String uuid;
  private String hostName;
  private String serviceTemplateName;

  @Override
  public String getName() {
    return uuid;
    //    return hostName + ":" + serviceTemplateName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.INSTANCE;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(INSTANCE, this);
    return map;
  }

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
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

  public static final class Builder {
    private String uuid;
    private String hostName;
    private String serviceTemplateName;

    private Builder() {}

    public static Builder anInstanceElement() {
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

    public Builder withServiceTemplateName(String serviceTemplateName) {
      this.serviceTemplateName = serviceTemplateName;
      return this;
    }

    public Builder but() {
      return anInstanceElement().withUuid(uuid).withHostName(hostName).withServiceTemplateName(serviceTemplateName);
    }

    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setHostName(hostName);
      instanceElement.setServiceTemplateName(serviceTemplateName);
      return instanceElement;
    }
  }
}
