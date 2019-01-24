package software.wings.helpers.ext.ecs.request;

import static software.wings.helpers.ext.ecs.request.EcsCommandRequest.EcsCommandType.ROUTE53_DNS_WEIGHT_UPDATE;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGRoute53DNSWeightUpdateRequest extends EcsCommandRequest {
  private boolean rollback;
  private String serviceName;
  private String serviceNameDownsized;
  private int serviceCountDownsized;
  private boolean downsizeOldService;
  private int oldServiceWeight;
  private int newServiceWeight;
  private String parentRecordName;
  private String parentRecordHostedZoneId;
  private String oldServiceDiscoveryArn;
  private String newServiceDiscoveryArn;
  private int timeout;
  private int ttl;

  @Builder
  public EcsBGRoute53DNSWeightUpdateRequest(String accountId, String appId, String commandName, String activityId,
      String region, String cluster, AwsConfig awsConfig, boolean rollback, String serviceName,
      String serviceNameDownsized, int serviceCountDownsized, boolean downsizeOldService, int oldServiceWeight,
      int newServiceWeight, String parentRecordName, String parentRecordHostedZoneId, String oldServiceDiscoveryArn,
      String newServiceDiscoveryArn, int timeout, int ttl) {
    super(accountId, appId, commandName, activityId, region, cluster, awsConfig, ROUTE53_DNS_WEIGHT_UPDATE);
    this.rollback = rollback;
    this.serviceName = serviceName;
    this.serviceNameDownsized = serviceNameDownsized;
    this.serviceCountDownsized = serviceCountDownsized;
    this.downsizeOldService = downsizeOldService;
    this.oldServiceWeight = oldServiceWeight;
    this.newServiceWeight = newServiceWeight;
    this.parentRecordName = parentRecordName;
    this.parentRecordHostedZoneId = parentRecordHostedZoneId;
    this.oldServiceDiscoveryArn = oldServiceDiscoveryArn;
    this.newServiceDiscoveryArn = newServiceDiscoveryArn;
    this.timeout = timeout;
    this.ttl = ttl;
  }
}