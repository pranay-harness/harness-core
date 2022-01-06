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
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLPhysicalHostInstance;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class PhysicalHostInstanceController implements InstanceController<QLPhysicalHostInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLPhysicalHostInstance populateInstance(Instance instance) {
    PhysicalHostInstanceInfo info = (PhysicalHostInstanceInfo) instance.getInstanceInfo();
    return QLPhysicalHostInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.PHYSICAL_HOST_INSTANCE)
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .hostPublicDns(info.getHostPublicDns())
        .build();
  }
}
