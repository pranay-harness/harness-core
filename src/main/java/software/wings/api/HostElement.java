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
  private String hostName;

  @Override
  public String getName() {
    return hostName;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.HOST;
  }

  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(HOST_OBJECT_NAME, this);
    return map;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("hostName", hostName).toString();
  }

  public static final class Builder {
    private String hostName;

    private Builder() {}

    public static Builder aHostElement() {
      return new Builder();
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder but() {
      return aHostElement().withHostName(hostName);
    }

    public HostElement build() {
      HostElement hostElement = new HostElement();
      hostElement.setHostName(hostName);
      return hostElement;
    }
  }
}
