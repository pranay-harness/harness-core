/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.cloudProvider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.health.CEHealthStatus;

import software.wings.graphql.schema.type.QLUser;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLCloudProviderBuilder {
  QLCloudProviderBuilder id(String id);
  QLCloudProviderBuilder name(String name);
  QLCloudProviderBuilder createdAt(Long createdAt);
  QLCloudProviderBuilder createdBy(QLUser createdBy);
  QLCloudProviderBuilder type(String type);
  QLCloudProviderBuilder isContinuousEfficiencyEnabled(boolean isContinuousEfficiencyEnabled);
  QLCloudProviderBuilder ceHealthStatus(CEHealthStatus ceHealthStatus);
  QLCloudProvider build();
}
