/**
 *
 */

package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

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
  public Map<String, Object> paramMap(ExecutionContext context) {
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

  @Override
  public String getUuid() {
    return null;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {}

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class ServiceInstanceIdsParamBuilder {
    private String serviceId;
    private List<String> instanceIds;

    private ServiceInstanceIdsParamBuilder() {}

    public static ServiceInstanceIdsParamBuilder aServiceInstanceIdsParam() {
      return new ServiceInstanceIdsParamBuilder();
    }

    public ServiceInstanceIdsParamBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public ServiceInstanceIdsParamBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public ServiceInstanceIdsParam build() {
      ServiceInstanceIdsParam serviceInstanceIdsParam = new ServiceInstanceIdsParam();
      serviceInstanceIdsParam.setServiceId(serviceId);
      serviceInstanceIdsParam.setInstanceIds(instanceIds);
      return serviceInstanceIdsParam;
    }
  }
}
