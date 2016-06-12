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
  private String displayName;
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

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public HostElement getHostElement() {
    return hostElement;
  }

  public void setHostElement(HostElement hostElement) {
    this.hostElement = hostElement;
  }

  public ServiceTemplateElement getServiceTemplateElement() {
    return serviceTemplateElement;
  }

  public void setServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
    this.serviceTemplateElement = serviceTemplateElement;
  }

  public static final class Builder {
    private String uuid;
    private String displayName;
    private HostElement hostElement;
    private ServiceTemplateElement serviceTemplateElement;

    private Builder() {}

    public static Builder anInstanceElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withHostElement(HostElement hostElement) {
      this.hostElement = hostElement;
      return this;
    }

    public Builder withServiceTemplateElement(ServiceTemplateElement serviceTemplateElement) {
      this.serviceTemplateElement = serviceTemplateElement;
      return this;
    }

    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setDisplayName(displayName);
      instanceElement.setHostElement(hostElement);
      instanceElement.setServiceTemplateElement(serviceTemplateElement);
      return instanceElement;
    }
  }
}
