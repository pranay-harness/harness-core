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

  public static final class EcsSetupParamsBuilder {
    private String taskFamily;
    private String serviceName;
    private boolean useLoadBalancer;
    private String appName;
    private String roleArn;
    private String envName;
    private String targetGroupArn;
    private ImageDetails imageDetails;
    private String loadBalancerName;
    private ContainerTask containerTask;
    private String region;
    private String infraMappingId;

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

    public EcsSetupParamsBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public EcsSetupParamsBuilder withRoleArn(String roleArn) {
      this.roleArn = roleArn;
      return this;
    }

    public EcsSetupParamsBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public EcsSetupParamsBuilder withTargetGroupArn(String targetGroupArn) {
      this.targetGroupArn = targetGroupArn;
      return this;
    }

    public EcsSetupParamsBuilder withImageDetails(ImageDetails imageDetails) {
      this.imageDetails = imageDetails;
      return this;
    }

    public EcsSetupParamsBuilder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
    }

    public EcsSetupParamsBuilder withContainerTask(ContainerTask containerTask) {
      this.containerTask = containerTask;
      return this;
    }

    public EcsSetupParamsBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public EcsSetupParamsBuilder withInfraMappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    public EcsSetupParamsBuilder but() {
      return anEcsSetupParams()
          .withTaskFamily(taskFamily)
          .withServiceName(serviceName)
          .withUseLoadBalancer(useLoadBalancer)
          .withAppName(appName)
          .withRoleArn(roleArn)
          .withEnvName(envName)
          .withTargetGroupArn(targetGroupArn)
          .withImageDetails(imageDetails)
          .withLoadBalancerName(loadBalancerName)
          .withContainerTask(containerTask)
          .withRegion(region)
          .withInfraMappingId(infraMappingId);
    }

    public EcsSetupParams build() {
      EcsSetupParams ecsSetupParams = new EcsSetupParams();
      ecsSetupParams.setTaskFamily(taskFamily);
      ecsSetupParams.setServiceName(serviceName);
      ecsSetupParams.setUseLoadBalancer(useLoadBalancer);
      ecsSetupParams.setAppName(appName);
      ecsSetupParams.setRoleArn(roleArn);
      ecsSetupParams.setEnvName(envName);
      ecsSetupParams.setTargetGroupArn(targetGroupArn);
      ecsSetupParams.setImageDetails(imageDetails);
      ecsSetupParams.setLoadBalancerName(loadBalancerName);
      ecsSetupParams.setContainerTask(containerTask);
      ecsSetupParams.setRegion(region);
      ecsSetupParams.setInfraMappingId(infraMappingId);
      return ecsSetupParams;
    }
  }
}
