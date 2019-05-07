package software.wings.graphql.datafetcher.instance.instanceInfo;

import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLEc2InstanceInfo;

public class CodeDeployInstanceInfoController implements InstanceInfoController<CodeDeployInstanceInfo> {
  @Override
  public void populateInstanceInfo(CodeDeployInstanceInfo info, QLInstanceBuilder builder) {
    builder.ec2InstanceInfo(QLEc2InstanceInfo.builder()
                                .hostPublicDns(info.getHostPublicDns())
                                .hostId(info.getHostId())
                                .hostName(info.getHostName())
                                .deploymentId(info.getDeploymentId())
                                .build());
  }
}
