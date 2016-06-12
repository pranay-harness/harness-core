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
 * The Class ServiceElement.
 *
 * @author Rishi
 */
public class ServiceTemplateElement implements ContextElement {
  private String uuid;
  private String name;

  private ServiceElement serviceElement;

  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.SERVICE_TEMPLATE;
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

  public ServiceElement getServiceElement() {
    return serviceElement;
  }

  public void setServiceElement(ServiceElement serviceElement) {
    this.serviceElement = serviceElement;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE_TEMPLATE, this);
    map.putAll(serviceElement.paramMap());
    return map;
  }

  public static final class Builder {
    private String uuid;
    private String name;
    private ServiceElement serviceElement;

    private Builder() {}

    public static Builder aServiceTemplateElement() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withServiceElement(ServiceElement serviceElement) {
      this.serviceElement = serviceElement;
      return this;
    }

    public ServiceTemplateElement build() {
      ServiceTemplateElement serviceTemplateElement = new ServiceTemplateElement();
      serviceTemplateElement.setUuid(uuid);
      serviceTemplateElement.setName(name);
      serviceTemplateElement.setServiceElement(serviceElement);
      return serviceTemplateElement;
    }
  }
}
