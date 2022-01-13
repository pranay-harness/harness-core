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
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLEc2Instance;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class Ec2InstanceController implements InstanceController<QLEc2Instance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLEc2Instance populateInstance(Instance instance) {
    Ec2InstanceInfo info = (Ec2InstanceInfo) instance.getInstanceInfo();

    return QLEc2Instance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.EC2_INSTANCE)
        .hostPublicDns(info.getHostPublicDns())
        .hostId(info.getHostId())
        .hostName(info.getHostName())
        .build();
  }
}
