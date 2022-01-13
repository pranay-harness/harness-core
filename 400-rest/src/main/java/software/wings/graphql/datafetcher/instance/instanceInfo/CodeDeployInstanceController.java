/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.instance.instanceInfo;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLCodeDeployInstance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
