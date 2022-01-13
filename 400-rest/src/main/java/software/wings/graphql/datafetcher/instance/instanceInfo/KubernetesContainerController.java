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
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLK8SPodInstance;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class KubernetesContainerController implements InstanceController<QLK8SPodInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLK8SPodInstance populateInstance(Instance instance) {
    KubernetesContainerInfo info = (KubernetesContainerInfo) instance.getInstanceInfo();

    return QLK8SPodInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.KUBERNETES_CONTAINER_INSTANCE)
        .clusterName(info.getClusterName())
        .ip(info.getIp())
        .podName(info.getPodName())
        .namespace(info.getNamespace())
        .build();
  }
}
