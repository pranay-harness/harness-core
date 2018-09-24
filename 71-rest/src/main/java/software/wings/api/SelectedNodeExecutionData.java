package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import software.wings.beans.ServiceInstance;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 4/3/17.
 */
public class SelectedNodeExecutionData extends StateExecutionData {
  private List<ServiceInstance> serviceInstanceList;
  private boolean excludeSelectedHostsFromFuturePhases;

  public List<ServiceInstance> getServiceInstanceList() {
    return serviceInstanceList;
  }

  public void setServiceInstanceList(List<ServiceInstance> serviceInstanceList) {
    this.serviceInstanceList = serviceInstanceList;
  }

  public boolean isExcludeSelectedHostsFromFuturePhases() {
    return excludeSelectedHostsFromFuturePhases;
  }

  public void setExcludeSelectedHostsFromFuturePhases(boolean excludeSelectedHostsFromFuturePhases) {
    this.excludeSelectedHostsFromFuturePhases = excludeSelectedHostsFromFuturePhases;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    if (isNotEmpty(serviceInstanceList)) {
      putNotNull(executionDetails, "hosts",
          ExecutionDataValue.builder()
              .displayName("Hosts")
              .value(serviceInstanceList.stream().map(ServiceInstance::getPublicDns).collect(toList()))
              .build());
    }
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    if (isNotEmpty(serviceInstanceList)) {
      putNotNull(executionDetails, "hosts",
          ExecutionDataValue.builder()
              .displayName("Hosts")
              .value(serviceInstanceList.stream().map(ServiceInstance::getPublicDns).collect(toList()))
              .build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    SelectNodeStepExecutionSummary selectNodeStepExecutionSummary = new SelectNodeStepExecutionSummary();
    populateStepExecutionSummary(selectNodeStepExecutionSummary);
    selectNodeStepExecutionSummary.setServiceInstanceList(serviceInstanceList);
    selectNodeStepExecutionSummary.setExcludeSelectedHostsFromFuturePhases(excludeSelectedHostsFromFuturePhases);
    return selectNodeStepExecutionSummary;
  }
}
