/**
 *
 */

package software.wings.api;

import com.google.common.base.MoreObjects;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class HostElement.
 *
 * @author Rishi
 */
public class HostElement implements ContextElement {
  private String uuid;
  private String hostName;

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

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HOST;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(HOST, this);
    return map;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String hostName;

    private Builder() {}

    /**
     * A host element builder.
     *
     * @return the builder
     */
    public static Builder aHostElement() {
      return new Builder();
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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHostElement().withHostName(hostName);
    }

    /**
     * Build host element.
     *
     * @return the host element
     */
    public HostElement build() {
      HostElement hostElement = new HostElement();
      hostElement.setHostName(hostName);
      return hostElement;
    }
  }
}
