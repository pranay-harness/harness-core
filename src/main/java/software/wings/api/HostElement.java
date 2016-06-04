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

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(HOST_OBJECT_NAME, this);
    return map;
  }
}
