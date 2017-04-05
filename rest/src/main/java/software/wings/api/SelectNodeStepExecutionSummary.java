package software.wings.api;

import software.wings.beans.ServiceInstance;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

/**
 * Created by rishi on 4/4/17.
 */
public class SelectNodeStepExecutionSummary extends StepExecutionSummary {
  private List<ServiceInstance> serviceInstanceList;

  public List<ServiceInstance> getServiceInstanceList() {
    return serviceInstanceList;
  }

  public void setServiceInstanceList(List<ServiceInstance> serviceInstanceList) {
    this.serviceInstanceList = serviceInstanceList;
  }
}
