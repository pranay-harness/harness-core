/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance.sync;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public interface ContainerSync {
  ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest);

  ContainerSyncResponse getInstances(
      ContainerInfrastructureMapping containerInfraMapping, List<ContainerMetadata> containerMetadataList);

  Set<String> getControllerNames(
      ContainerInfrastructureMapping containerInfraMapping, Map<String, String> labels, String namespace);
}
