/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
