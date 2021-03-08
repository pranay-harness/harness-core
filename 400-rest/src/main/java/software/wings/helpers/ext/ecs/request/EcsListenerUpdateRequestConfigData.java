package software.wings.helpers.ext.ecs.request;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class EcsListenerUpdateRequestConfigData {
  private String prodListenerArn;
  private String stageListenerArn;
  private boolean isUseSpecificRuleArn;
  private String prodListenerRuleArn;
  private String stageListenerRuleArn;
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
