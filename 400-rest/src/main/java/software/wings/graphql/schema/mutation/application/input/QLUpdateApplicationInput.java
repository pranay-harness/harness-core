/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.mutation.application.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateApplicationInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private RequestField<String> name;
  private RequestField<String> description;
  private Boolean isManualTriggerAuthorized;
}
