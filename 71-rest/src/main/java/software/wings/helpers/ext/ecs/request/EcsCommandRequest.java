package software.wings.helpers.ext.ecs.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.AwsConfig;

@Data
@AllArgsConstructor
public class EcsCommandRequest {
  private String accountId;
  private String appId;
  private String commandName;
  private String activityId;
  private String region;
  private String cluster;
  private AwsConfig AwsConfig;
  @NotEmpty private EcsCommandType EcsCommandType;

  public enum EcsCommandType {
    LISTENER_UPDATE_BG,
    BG_SERVICE_SETUP,
    SERVICE_SETUP,
    ROUTE53_BG_SERVICE_SETUP,
    ROUTE53_DNS_WEIGHT_UPDATE
  }
}
