package software.wings.helpers.ext.ecs.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGListenerUpdateRequest extends EcsCommandRequest {
  private String prodListenerArn;
  private String stageListenerArn;
  private boolean isUseSpecificListenerRuleArn;
  private String prodListenerRuleArn;
  private String stageListenerRuleArn;
  private String targetGroupArn1;
  private String targetGroupArn2;
  private String serviceName;
  private String serviceNameDownsized;
  private int serviceCountDownsized;
  private boolean rollback;
  private boolean downsizeOldService;
  private String targetGroupForNewService;
  private String targetGroupForExistingService;
  private int serviceSteadyStateTimeout;

  @Builder
  public EcsBGListenerUpdateRequest(String commandName, String appId, String accountId, String activityId,
      String prodListenerArn, String stageListenerArn, String prodListenerRuleArn, String stageListenerRuleArn,
      String targetGroupArn1, String targetGroupArn2, String serviceName, String clusterName, String region,
      String serviceNameDownsized, int serviceCountDownsized, AwsConfig awsConfig, boolean rollback,
      boolean downsizeOldService, boolean isUseSpecificListenerRuleArn, String targetGroupForNewService,
      String targetGroupForExistingService, int serviceSteadyStateTimeout) {
    super(accountId, appId, commandName, activityId, region, clusterName, awsConfig, EcsCommandType.LISTENER_UPDATE_BG);
    this.prodListenerArn = prodListenerArn;
    this.stageListenerArn = stageListenerArn;
    this.prodListenerRuleArn = prodListenerRuleArn;
    this.stageListenerRuleArn = stageListenerRuleArn;
    this.targetGroupArn1 = targetGroupArn1;
    this.targetGroupArn2 = targetGroupArn2;
    this.serviceName = serviceName;
    this.serviceNameDownsized = serviceNameDownsized;
    this.serviceCountDownsized = serviceCountDownsized;
    this.rollback = rollback;
    this.targetGroupForNewService = targetGroupForNewService;
    this.targetGroupForExistingService = targetGroupForExistingService;
    this.downsizeOldService = downsizeOldService;
    this.isUseSpecificListenerRuleArn = isUseSpecificListenerRuleArn;
    this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
  }
}
