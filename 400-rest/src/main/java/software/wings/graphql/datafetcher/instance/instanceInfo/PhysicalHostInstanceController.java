/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
