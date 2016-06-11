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

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, this);
    return map;
  }

  public static final class Builder {
    private String uuid;
    private String name;

    private Builder() {}

    public static Builder aServiceElement() {
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

    public Builder but() {
      return aServiceElement().withUuid(uuid).withName(name);
    }

    public ServiceElement build() {
      ServiceElement serviceElement = new ServiceElement();
      serviceElement.setUuid(uuid);
      serviceElement.setName(name);
      return serviceElement;
    }
  }
}
