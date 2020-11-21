package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLCodeDeployInstance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

public class CodeDeployInstanceController implements InstanceController<QLCodeDeployInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLCodeDeployInstance populateInstance(Instance instance) {
    CodeDeployInstanceInfo info = (CodeDeployInstanceInfo) instance.getInstanceInfo();
    return QLCodeDeployInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.CODE_DEPLOY_INSTANCE)
        .hostPublicDns(info.getHostPublicDns())
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .deploymentId(info.getDeploymentId())
        .build();
  }
}
