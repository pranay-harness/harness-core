/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.k8sLabel;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLK8sLabelFilter {
  private QLIdFilter accountId;
  private QLIdFilter cluster;
  private QLIdFilter namespace;
  private QLIdFilter workloadName;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
}
