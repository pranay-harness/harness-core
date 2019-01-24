package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;

import java.util.List;

@Data
@Builder
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

  // Only for ECS BG route 53 DNS swap
  private boolean useRoute53DNSSwap;
  private String serviceDiscoveryService1JSON;
  private String serviceDiscoveryService2JSON;
  private String parentRecordHostedZoneId;
  private String parentRecordName;
}