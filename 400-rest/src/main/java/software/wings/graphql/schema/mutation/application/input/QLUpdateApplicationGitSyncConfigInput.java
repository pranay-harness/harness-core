/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.mutation.application.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationInput;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateApplicationGitSyncConfigInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateApplicationGitSyncConfigInput implements QLMutationInput {
  private String clientMutationId;
  private String applicationId;
  private String gitConnectorId;
  private String branch;
  private String repositoryName;
  private Boolean syncEnabled;
}
