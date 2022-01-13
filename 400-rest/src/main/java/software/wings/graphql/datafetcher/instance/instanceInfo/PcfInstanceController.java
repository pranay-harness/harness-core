/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.instance.instanceInfo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceControllerUtils;
import software.wings.graphql.schema.type.instance.QLInstanceType;
import software.wings.graphql.schema.type.instance.QLPcfInstance;

import com.google.inject.Inject;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDP)
public class PcfInstanceController implements InstanceController<QLPcfInstance> {
  @Inject InstanceControllerUtils util;

  @Override
  public QLPcfInstance populateInstance(Instance instance) {
    PcfInstanceInfo info = (PcfInstanceInfo) instance.getInstanceInfo();
    return QLPcfInstance.builder()
        .id(instance.getUuid())
        .applicationId(instance.getAppId())
        .environmentId(instance.getEnvId())
        .serviceId(instance.getServiceId())
        .artifact(util.getQlArtifact(instance))
        .type(QLInstanceType.PCF_INSTANCE)

        .pcfId(info.getId())
        .instanceIndex(info.getInstanceIndex())
        .organization(info.getOrganization())
        .pcfApplicationGuid(info.getPcfApplicationGuid())
        .pcfApplicationName(info.getPcfApplicationName())
        .space(info.getSpace())
        .build();
  }
}
