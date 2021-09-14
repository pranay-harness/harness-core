/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.QLK8sLabel;
import software.wings.graphql.schema.type.QLTags;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLFilterValuesData implements QLData {
  List<QLEntityData> cloudServiceNames;
  List<QLEntityData> launchTypes;
  List<QLEntityData> taskIds;
  List<QLEntityData> namespaces;
  List<QLEntityData> workloadNames;
  List<QLEntityData> cloudProviders;
  List<QLEntityData> applications;
  List<QLEntityData> environments;
  List<QLEntityData> services;
  List<QLEntityData> clusters;
  List<QLEntityData> instances;
  List<QLK8sLabel> k8sLabels;
  List<QLTags> tags;
}
