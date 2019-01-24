package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsSetupParams extends ContainerSetupParams {
  private String taskFamily;
  private boolean useLoadBalancer;
  private String roleArn;
  private String targetGroupArn;
  private String targetGroupArn2;
  private String prodListenerArn;
  private String stageListenerArn;
  private String loadBalancerName;
  private String region;
  private String vpcId;
  private String[] subnetIds;
  private String[] securityGroupIds;
  private boolean assignPublicIps;
  private String executionRoleArn;
  private String launchType;
  private String targetContainerName;
  private String targetPort;
  private boolean rollback;
  private String previousEcsServiceSnapshotJson;
  private String ecsServiceArn;
  private EcsServiceSpecification ecsServiceSpecification;
  private boolean isDaemonSchedulingStrategy;
  private List<AwsAutoScalarConfig> newAwsAutoScalarConfigList;
  private List<AwsAutoScalarConfig> previousAutoScalarConfigList;
  private boolean blueGreen;
  private String stageListenerPort;

  // Only for ECS BG route 53 DNS swap
  private boolean useRoute53DNSSwap;
  private String serviceDiscoveryService1JSON;
  private String serviceDiscoveryService2JSON;
  private String parentRecordHostedZoneId;
  private String parentRecordName;

  public static final class EcsSetupParamsBuilder {
    private String taskFamily;
    private String serviceName;
    private boolean useLoadBalancer;
    private String clusterName;
    private String roleArn;
    private String appName;
    private String targetGroupArn;
    private String envName;
    private String loadBalancerName;
    private ImageDetails imageDetails;
    private String region;
    private ContainerTask containerTask;
    private String vpcId;
    private String infraMappingId;
    private String[] subnetIds;
    private int serviceSteadyStateTimeout;
    private String[] securityGroupIds;
    private boolean assignPublicIps;
    private String executionRoleArn;
    private String launchType;
    private String targetContainerName;
    private String targetPort;
    private boolean rollback;
    private String previousEcsServiceSnapshotJson;
    private String ecsServiceArn;
    private EcsServiceSpecification ecsServiceSpecification;
    private boolean isDaemonSchedulingStrategy;
    private List<AwsAutoScalarConfig> newAwsAutoScalarConfigList;
    private List<AwsAutoScalarConfig> previousAutoScalarConfigList;
    private boolean blueGreen;
    private String targetGroupArn2;
    private String prodListenerArn;
    private String stageListenerArn;
    private String stageListenerPort;

    // Only for ECS BG route 53 DNS swap
    private boolean useRoute53DNSSwap;
    private String serviceDiscoveryService1JSON;
    private String serviceDiscoveryService2JSON;
    private String parentRecordHostedZoneId;
    private String parentRecordName;

    private EcsSetupParamsBuilder() {}

    public static EcsSetupParamsBuilder anEcsSetupParams() {
      return new EcsSetupParamsBuilder();
    }

    public EcsSetupParamsBuilder withTaskFamily(String taskFamily) {
      this.taskFamily = taskFamily;
      return this;
    }

    public EcsSetupParamsBuilder withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public EcsSetupParamsBuilder withUseLoadBalancer(boolean useLoadBalancer) {
      this.useLoadBalancer = useLoadBalancer;
      return this;
    }

    public EcsSetupParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public EcsSetupParamsBuilder withRoleArn(String roleArn) {
      this.roleArn = roleArn;
      return this;
    }

    public EcsSetupParamsBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public EcsSetupParamsBuilder withTargetGroupArn(String targetGroupArn) {
      this.targetGroupArn = targetGroupArn;
      return this;
    }

    public EcsSetupParamsBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public EcsSetupParamsBuilder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public EcsSetupParamsBuilder withImageDetails(ImageDetails imageDetails) {
      this.imageDetails = imageDetails;
      return this;
    }

    public EcsSetupParamsBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public EcsSetupParamsBuilder withContainerTask(ContainerTask containerTask) {
      this.containerTask = containerTask;
      return this;
    }

    public EcsSetupParamsBuilder withVpcId(String vpcId) {
      this.vpcId = vpcId;
      return this;
    }

    public EcsSetupParamsBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public EcsSetupParamsBuilder withSubnetIds(String[] subnetIds) {
      this.subnetIds = subnetIds.clone();
      return this;
    }

    public EcsSetupParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public EcsSetupParamsBuilder withSecurityGroupIds(String[] securityGroupIds) {
      this.securityGroupIds = securityGroupIds.clone();
      return this;
    }

    public EcsSetupParamsBuilder withAssignPublicIps(boolean assignPublicIps) {
      this.assignPublicIps = assignPublicIps;
      return this;
    }

    public EcsSetupParamsBuilder withExecutionRoleArn(String executionRoleArn) {
      this.executionRoleArn = executionRoleArn;
      return this;
    }

    public EcsSetupParamsBuilder withLaunchType(String launchType) {
      this.launchType = launchType;
      return this;
    }

    public EcsSetupParamsBuilder withTargetContainerName(String targetContainerName) {
      this.targetContainerName = targetContainerName;
      return this;
    }

    public EcsSetupParamsBuilder withTargetPort(String targetPort) {
      this.targetPort = targetPort;
      return this;
    }

    public EcsSetupParamsBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public EcsSetupParamsBuilder withPreviousEcsServiceSnapshotJson(String previousEcsServiceSnapshotJson) {
      this.previousEcsServiceSnapshotJson = previousEcsServiceSnapshotJson;
      return this;
    }

    public EcsSetupParamsBuilder withEcsServiceSpecification(EcsServiceSpecification ecsServiceSpecification) {
      this.ecsServiceSpecification = ecsServiceSpecification;
      return this;
    }

    public EcsSetupParamsBuilder withEcsServiceArn(String ecsServiceArn) {
      this.ecsServiceArn = ecsServiceArn;
      return this;
    }

    public EcsSetupParamsBuilder withIsDaemonSchedulingStrategy(boolean isDaemonSchedulingStrategy) {
      this.isDaemonSchedulingStrategy = isDaemonSchedulingStrategy;
      return this;
    }

    public EcsSetupParamsBuilder withPreviousAutoScalarConfigList(
        List<AwsAutoScalarConfig> previousAutoScalarConfigList) {
      this.previousAutoScalarConfigList = previousAutoScalarConfigList;
      return this;
    }

    public EcsSetupParamsBuilder withNewAwsAutoScalarConfigList(List<AwsAutoScalarConfig> newAwsAutoScalarConfigList) {
      this.newAwsAutoScalarConfigList = newAwsAutoScalarConfigList;
      return this;
    }

    public EcsSetupParamsBuilder withBlueGreen(boolean blueGreen) {
      this.blueGreen = blueGreen;
      return this;
    }

    public EcsSetupParamsBuilder withProdListenerArn(String prodListenerArn) {
      this.prodListenerArn = prodListenerArn;
      return this;
    }

    public EcsSetupParamsBuilder withStageListenerArn(String stageListenerArn) {
      this.stageListenerArn = stageListenerArn;
      return this;
    }

    public EcsSetupParamsBuilder withTargetGroupArn2(String targetGroupArn2) {
      this.targetGroupArn2 = targetGroupArn2;
      return this;
    }

    public EcsSetupParamsBuilder withStageListenerPort(String stageListenerPort) {
      this.stageListenerPort = stageListenerPort;
      return this;
    }

    public EcsSetupParamsBuilder withUseDNSRoute53Swap(boolean useRoute53DNSSwap) {
      this.useRoute53DNSSwap = useRoute53DNSSwap;
      return this;
    }

    public EcsSetupParamsBuilder withServiceDiscoveryService1JSON(String serviceDiscoveryService1JSON) {
      this.serviceDiscoveryService1JSON = serviceDiscoveryService1JSON;
      return this;
    }

    public EcsSetupParamsBuilder withServiceDiscoveryService2JSON(String serviceDiscoveryService2JSON) {
      this.serviceDiscoveryService2JSON = serviceDiscoveryService2JSON;
      return this;
    }

    public EcsSetupParamsBuilder withParentRecordHostedZoneId(String parentRecordHostedZoneId) {
      this.parentRecordHostedZoneId = parentRecordHostedZoneId;
      return this;
    }

    public EcsSetupParamsBuilder withParentRecordName(String parentRecordName) {
      this.parentRecordName = parentRecordName;
      return this;
    }

    public EcsSetupParams build() {
      EcsSetupParams ecsSetupParams = new EcsSetupParams();
      ecsSetupParams.setTaskFamily(taskFamily);
      ecsSetupParams.setServiceName(serviceName);
      ecsSetupParams.setUseLoadBalancer(useLoadBalancer);
      ecsSetupParams.setClusterName(clusterName);
      ecsSetupParams.setRoleArn(roleArn);
      ecsSetupParams.setAppName(appName);
      ecsSetupParams.setTargetGroupArn(targetGroupArn);
      ecsSetupParams.setEnvName(envName);
      ecsSetupParams.setLoadBalancerName(loadBalancerName);
      ecsSetupParams.setImageDetails(imageDetails);
      ecsSetupParams.setRegion(region);
      ecsSetupParams.setContainerTask(containerTask);
      ecsSetupParams.setVpcId(vpcId);
      ecsSetupParams.setInfraMappingId(infraMappingId);
      ecsSetupParams.setSubnetIds(subnetIds);
      ecsSetupParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      ecsSetupParams.setSecurityGroupIds(securityGroupIds);
      ecsSetupParams.setAssignPublicIps(assignPublicIps);
      ecsSetupParams.setExecutionRoleArn(executionRoleArn);
      ecsSetupParams.setLaunchType(launchType);
      ecsSetupParams.setTargetContainerName(targetContainerName);
      ecsSetupParams.setTargetPort(targetPort);
      ecsSetupParams.setRollback(rollback);
      ecsSetupParams.setPreviousEcsServiceSnapshotJson(previousEcsServiceSnapshotJson);
      ecsSetupParams.setEcsServiceSpecification(ecsServiceSpecification);
      ecsSetupParams.setEcsServiceArn(ecsServiceArn);
      ecsSetupParams.setDaemonSchedulingStrategy(isDaemonSchedulingStrategy);
      ecsSetupParams.setNewAwsAutoScalarConfigList(newAwsAutoScalarConfigList);
      ecsSetupParams.setPreviousAutoScalarConfigList(previousAutoScalarConfigList);
      ecsSetupParams.setBlueGreen(blueGreen);
      ecsSetupParams.setTargetGroupArn2(targetGroupArn2);
      ecsSetupParams.setProdListenerArn(prodListenerArn);
      ecsSetupParams.setStageListenerArn(stageListenerArn);
      ecsSetupParams.setStageListenerPort(stageListenerPort);
      ecsSetupParams.setUseRoute53DNSSwap(useRoute53DNSSwap);
      ecsSetupParams.setServiceDiscoveryService1JSON(serviceDiscoveryService1JSON);
      ecsSetupParams.setServiceDiscoveryService2JSON(serviceDiscoveryService2JSON);
      ecsSetupParams.setParentRecordHostedZoneId(parentRecordHostedZoneId);
      ecsSetupParams.setParentRecordName(parentRecordName);
      return ecsSetupParams;
    }
  }
}