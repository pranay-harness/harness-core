package software.wings.beans.command;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 2/3/17.
 */
public class ResizeCommandUnit extends ContainerOrchestrationCommandUnit {
  @Inject @Transient protected transient AwsClusterService awsClusterService;

  public ResizeCommandUnit() {
    super(CommandUnitType.RESIZE);
    setDeploymentType(DeploymentType.ECS.name());
  }

  @Override
  protected List<ContainerInfo> executeInternal(String region, SettingAttribute cloudProviderSetting,
      String clusterName, String namespace, String serviceName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback) {
    return awsClusterService.resizeCluster(region, cloudProviderSetting, clusterName, serviceName, previousCount,
        desiredCount, serviceSteadyStateTimeout, executionLogCallback);
  }
}
