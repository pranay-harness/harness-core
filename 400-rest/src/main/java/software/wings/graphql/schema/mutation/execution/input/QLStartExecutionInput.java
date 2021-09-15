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

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerExecutionInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLStartExecutionInput implements QLMutationInput {
  String clientMutationId;
  String applicationId;
  String entityId;
  QLExecutionType executionType;
  List<QLVariableInput> variableInputs;
  List<QLServiceInput> serviceInputs;
  String notes;
  boolean excludeHostsWithSameArtifact;
  boolean targetToSpecificHosts;
  List<String> specificHosts;
  boolean continueWithDefaultValues;
}
