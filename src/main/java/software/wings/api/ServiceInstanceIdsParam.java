/**
 *
 */

package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.List;
import java.util.Map;

/**
 * The type Service instance ids param.
 *
 * @author Rishi
 */
public class ServiceInstanceIdsParam implements ContextElement {
  private String serviceId;
  private List<String> instanceIds;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return Constants.SERVICE_INSTANCE_IDS_PARAMS;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  /**
   * Gets instance ids.
   *
   * @return the instance ids
   */
  public List<String> getInstanceIds() {
    return instanceIds;
  }

  /**
   * Sets instance ids.
   *
   * @param instanceIds the instance ids
   */
  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }
}
