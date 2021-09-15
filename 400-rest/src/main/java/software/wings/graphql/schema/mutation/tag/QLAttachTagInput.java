/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.tag;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.aggregation.QLEntityType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAttachTagInputKeys")
@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAttachTagInput implements QLMutationInput {
  private String clientMutationId;
  private String entityId;
  private String name;
  private String value;
  private QLEntityType entityType;
}
