/**
 *
 */

package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class InstanceElement.
 *
 * @author Rishi
 */
public class InstanceElement implements ContextElement {
  private String uuid;
  private String displayName;
  private String hostName;
  private HostElement hostElement;
  private ServiceTemplateElement serviceTemplateElement;

  @Override
  public String getName() {
    return displayName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.INSTANCE;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(INSTANCE, this);
    map.putAll(hostElement.paramMap());
    map.putAll(serviceTemplateElement.paramMap());
    return map;
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

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets host element.
   *
   * @return the host element
   */
  public HostElement getHostElement() {
    return hostElement;
  }

  /**
   * Sets host element.
   *
   * @param hostElement the host element
   */
  public void setHostElement(HostElement hostElement) {
    this.hostElement = hostElement;
  }

  /**
   * Gets service template element.
   *
   * @return the service template element
   */
  public ServiceTemplateElement getServiceTemplateElement() {
    return serviceTemplateElement;
  }

  /**
   * Sets service template element.
   *
   * @param serviceTemplateElement the service template element
   */
  public void setServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
    this.serviceTemplateElement = serviceTemplateElement;
  }

  @Override
  public String toString() {
    return "InstanceElement{"
        + "uuid='" + uuid + '\'' + ", displayName='" + displayName + '\'' + ", hostName='" + hostName + '\''
        + ", hostElement=" + hostElement + ", serviceTemplateElement=" + serviceTemplateElement + '}';
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String uuid;
    private String displayName;
    private String hostName;
    private HostElement hostElement;
    private ServiceTemplateElement serviceTemplateElement;

    private Builder() {}

    /**
     * An instance element builder.
     *
     * @return the builder
     */
    public static Builder anInstanceElement() {
      return new Builder();
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
     * With display name builder.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With host element builder.
     *
     * @param hostElement the host element
     * @return the builder
     */
    public Builder withHostElement(HostElement hostElement) {
      this.hostElement = hostElement;
      return this;
    }

    /**
     * With service template element builder.
     *
     * @param serviceTemplateElement the service template element
     * @return the builder
     */
    public Builder withServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
      this.serviceTemplateElement = serviceTemplateElement;
      return this;
    }

    /**
     * Build instance element.
     *
     * @return the instance element
     */
    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setDisplayName(displayName);
      instanceElement.setHostName(hostName);
      instanceElement.setHostElement(hostElement);
      instanceElement.setServiceTemplateElement(serviceTemplateElement);
      return instanceElement;
    }
  }
}
