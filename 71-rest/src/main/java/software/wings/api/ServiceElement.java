/**
 *
 */

package software.wings.api;

import static software.wings.api.ServiceElement.Builder.aServiceElement;

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
public class ServiceElement implements ContextElement {
  private String uuid;
  private String name;

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
    return ContextElementType.SERVICE;
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

  @Override
  public ContextElement cloneMin() {
    return aServiceElement().withUuid(uuid).withName(name).build();
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, this);
    return map;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String uuid;
    private String name;

    private Builder() {}

    /**
     * A service element builder.
     *
     * @return the builder
     */
    public static Builder aServiceElement() {
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aServiceElement().withUuid(uuid).withName(name);
    }

    /**
     * Build service element.
     *
     * @return the service element
     */
    public ServiceElement build() {
      ServiceElement serviceElement = new ServiceElement();
      serviceElement.setUuid(uuid);
      serviceElement.setName(name);
      return serviceElement;
    }
  }
}
