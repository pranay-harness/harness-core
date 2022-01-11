/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app.schema.mutation.delegate.input;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
@Scope(PermissionAttribute.ResourceType.APPLICATION)

public class QLAttachScopeToDelegateInput implements QLMutationInput {
  String clientMutationId;

  String accountId;
  String delegateId;
  QLIdFilter includeScopes;
  QLIdFilter excludeScopes;
}
