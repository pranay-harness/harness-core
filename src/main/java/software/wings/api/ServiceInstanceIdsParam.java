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

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public List<String> getInstanceIds() {
    return instanceIds;
  }

  public void setInstanceIds(List<String> instanceIds) {
    this.instanceIds = instanceIds;
  }
}
