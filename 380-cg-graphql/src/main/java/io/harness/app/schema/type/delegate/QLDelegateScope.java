/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDelegateScope implements QLObject {
  String name;
  String accountId;
  String uuid;
  List<QLTaskGroup> taskTypes;
  List<QLEnvironmentType> environmentTypes;
  List<String> applications;
  List<String> environments;
  List<String> services;
  List<String> infrastructureDefinitions;
}
