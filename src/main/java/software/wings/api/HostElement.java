/**
 *
 */
package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rishi
 *
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
    map.put(ContextElementType.HOST.getDisplayName(), this);
    return map;
  }
}
