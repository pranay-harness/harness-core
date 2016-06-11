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

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public static final class Builder {
    private String uuid;
    private String displayName;

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

    public InstanceElement build() {
      InstanceElement instanceElement = new InstanceElement();
      instanceElement.setUuid(uuid);
      instanceElement.setDisplayName(displayName);
      return instanceElement;
    }
  }
}
