/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "QLVariableValueKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLVariableValue {
  QLVariableValueType type;
  String value;
}
