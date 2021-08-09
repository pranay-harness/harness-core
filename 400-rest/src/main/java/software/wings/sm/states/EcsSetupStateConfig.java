package software.wings.sm.states;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.ImageDetails;

import software.wings.beans.Application;
import software.wings.beans.AwsElbConfig;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class EcsSetupStateConfig {
  private String serviceName;
  private ImageDetails imageDetails;
  private Application app;
  private Environment env;
  private Service service;
  private InfrastructureMapping infrastructureMapping;
  private ContainerTask containerTask;
  private String clusterName;
  private String ecsServiceName;
  private boolean useLoadBalancer;
  private String loadBalancerName;
  private String targetGroupArn;
  // Only to be used for ECS BG
  private String targetGroupArn2;
  private String prodListenerArn;
  private String stageListenerArn;
  private String prodListenerRuleArn;
  private String stageListenerRuleArn;
  private boolean isUseSpecificListenerRuleArn;
  private String stageListenerPort;
  private boolean blueGreen;
  private String roleArn;
  private String targetContainerName;
  private String targetPort;
  private int serviceSteadyStateTimeout;
  private boolean rollback;
  private String previousEcsServiceSnapshotJson;
  private EcsServiceSpecification ecsServiceSpecification;
  private boolean isDaemonSchedulingStrategy;
  private String ecsServiceArn;
  private String orchestrationWorkflowType;
  private List<AwsAutoScalarConfig> awsAutoScalarConfigs;
  private List<AwsElbConfig> awsElbConfigs;
  private boolean isMultipleLoadBalancersFeatureFlagActive;

  // Only for ECS BG route 53 DNS swap
  private boolean useRoute53DNSSwap;
  private String serviceDiscoveryService1JSON;
  private String serviceDiscoveryService2JSON;
  private String parentRecordHostedZoneId;
  private String parentRecordName;
}
