/**
 *
 */
package software.wings.api;

import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * The Class InstanceElement.
 *
 * @author Rishi
 */
public class InstanceElement implements ContextElement {
  private String uuid;
  private String hostName;
  private String serviceTemplateName;

  @Override
  public String getName() {
    return hostName + ":" + serviceTemplateName;
  }

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.INSTANCE;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ContextElement#paramMap()
   */
  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getServiceTemplateName() {
    return serviceTemplateName;
  }

  public void setServiceTemplateName(String serviceTemplateName) {
    this.serviceTemplateName = serviceTemplateName;
  }
}
