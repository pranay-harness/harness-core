package software.wings.helpers.ext.ecs.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsListenerUpdateRequestConfigData {
  private String prodListenerArn;
  private String stageListenerArn;
  private String serviceName;
  private String clusterName;
  private String region;
  private String serviceNameDownsized;
  private int serviceCountDownsized;
  private boolean rollback;
  private boolean downsizeOldService;
  private String targetGroupForNewService;
  private String targetGroupForExistingService;
}