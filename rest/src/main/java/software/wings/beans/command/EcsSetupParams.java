package software.wings.beans.command;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;

@Data
@EqualsAndHashCode(callSuper = true)
public class EcsSetupParams extends ContainerSetupParams {
  private String taskFamily;
  private boolean useLoadBalancer;
  private String roleArn;
  private String targetGroupArn;
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

    public EcsSetupParamsBuilder but() {
      return anEcsSetupParams()
          .withTaskFamily(taskFamily)
          .withServiceName(serviceName)
          .withUseLoadBalancer(useLoadBalancer)
          .withClusterName(clusterName)
          .withRoleArn(roleArn)
          .withAppName(appName)
          .withTargetGroupArn(targetGroupArn)
          .withEnvName(envName)
          .withLoadBalancerName(loadBalancerName)
          .withImageDetails(imageDetails)
          .withRegion(region)
          .withContainerTask(containerTask)
          .withVpcId(vpcId)
          .withInfraMappingId(infraMappingId)
          .withSubnetIds(subnetIds)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withSecurityGroupIds(securityGroupIds)
          .withAssignPublicIps(assignPublicIps)
          .withExecutionRoleArn(executionRoleArn)
          .withLaunchType(launchType)
          .withTargetContainerName(targetContainerName)
          .withTargetPort(targetPort);
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
      return ecsSetupParams;
    }
  }
}
