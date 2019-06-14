package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class KubernetesSwapServiceSelectorsParams implements ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private ContainerServiceParams containerServiceParams;
  private String service1;
  private String service2;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return containerServiceParams.fetchRequiredExecutionCapabilities();
  }
}
