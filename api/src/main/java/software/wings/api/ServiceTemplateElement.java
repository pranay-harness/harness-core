/**
 *
 */

package software.wings.api;

import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ServiceElement.
 *
 * @author Rishi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

  /**
   * Gets service element.
   *
   * @return the service element
   */
  public ServiceElement getServiceElement() {
    return serviceElement;
  }

  /**
   * Sets service element.
   *
   * @param serviceElement the service element
   */
  public void setServiceElement(ServiceElement serviceElement) {
    this.serviceElement = serviceElement;
  }

  @Override
  public ContextElement cloneMin() {
    return aServiceTemplateElement().withUuid(uuid).withName(name).build();
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE_TEMPLATE, this);
    map.putAll(serviceElement.paramMap(context));
    return map;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String uuid;
    private String name;
    private ServiceElement serviceElement;

    private Builder() {}

    /**
     * A service template element builder.
     *
     * @return the builder
     */
    public static Builder aServiceTemplateElement() {
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
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With service element builder.
     *
     * @param serviceElement the service element
     * @return the builder
     */
    public Builder withServiceElement(ServiceElement serviceElement) {
      this.serviceElement = serviceElement;
      return this;
    }

    /**
     * Build service template element.
     *
     * @return the service template element
     */
    public ServiceTemplateElement build() {
      ServiceTemplateElement serviceTemplateElement = new ServiceTemplateElement();
      serviceTemplateElement.setUuid(uuid);
      serviceTemplateElement.setName(name);
      serviceTemplateElement.setServiceElement(serviceElement);
      return serviceTemplateElement;
    }
  }
}
