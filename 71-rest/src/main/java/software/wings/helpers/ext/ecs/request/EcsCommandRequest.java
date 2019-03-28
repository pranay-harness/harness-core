package software.wings.helpers.ext.ecs.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@AllArgsConstructor
public class EcsCommandRequest implements ExecutionCapabilityDemander {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private String region;
  private String cluster;
  private AwsConfig AwsConfig;
  @NotEmpty private EcsCommandType EcsCommandType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return AwsConfig.fetchRequiredExecutionCapabilities();
  }

  public enum EcsCommandType {
    LISTENER_UPDATE_BG,
    BG_SERVICE_SETUP,
    SERVICE_SETUP,
    ROUTE53_BG_SERVICE_SETUP,
    ROUTE53_DNS_WEIGHT_UPDATE,
    SERVICE_DEPLOY
  }
}