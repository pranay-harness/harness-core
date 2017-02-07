package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 2/6/17.
 */
public class ServiceInstancesProvisionState implements PhaseStepExecutionState {
  private String serviceId;
  private List<String> instanceIds = new ArrayList<>();

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

  public static final class ServiceInstancesProvisionStateBuilder {
    private String serviceId;
    private List<String> instanceIds = new ArrayList<>();

    private ServiceInstancesProvisionStateBuilder() {}

    public static ServiceInstancesProvisionStateBuilder aServiceInstancesProvisionState() {
      return new ServiceInstancesProvisionStateBuilder();
    }

    public ServiceInstancesProvisionStateBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public ServiceInstancesProvisionStateBuilder withInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
      return this;
    }

    public ServiceInstancesProvisionState build() {
      ServiceInstancesProvisionState serviceInstancesProvisionState = new ServiceInstancesProvisionState();
      serviceInstancesProvisionState.setServiceId(serviceId);
      serviceInstancesProvisionState.setInstanceIds(instanceIds);
      return serviceInstancesProvisionState;
    }
  }
}
