package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.beans.ServiceInstance;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by rishi on 4/3/17.
 */
public class SelectedNodeExecutionData extends StateExecutionData {
  private List<ServiceInstance> serviceInstanceList;

  public List<ServiceInstance> getServiceInstanceList() {
    return serviceInstanceList;
  }

  public void setServiceInstanceList(List<ServiceInstance> serviceInstanceList) {
    this.serviceInstanceList = serviceInstanceList;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    if (serviceInstanceList != null && !serviceInstanceList.isEmpty()) {
      putNotNull(executionDetails, "hosts",
          anExecutionDataValue()
              .withDisplayName("Hosts")
              .withValue(serviceInstanceList.stream().map(ServiceInstance::getHostName).collect(Collectors.toList()))
              .build());
    }
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    if (serviceInstanceList != null && !serviceInstanceList.isEmpty()) {
      putNotNull(executionDetails, "hosts",
          anExecutionDataValue()
              .withDisplayName("Hosts")
              .withValue(serviceInstanceList.stream().map(ServiceInstance::getHostName).collect(Collectors.toList()))
              .build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    SelectNodeStepExecutionSummary selectNodeStepExecutionSummary = new SelectNodeStepExecutionSummary();
    populateStepExecutionSummary(selectNodeStepExecutionSummary);
    selectNodeStepExecutionSummary.setServiceInstanceList(serviceInstanceList);
    return selectNodeStepExecutionSummary;
  }
}
