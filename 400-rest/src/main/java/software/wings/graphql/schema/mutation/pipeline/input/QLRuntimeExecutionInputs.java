/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.pipeline.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLRuntimeExecutionInputs {
  String clientMutationId;

  String applicationId;
  String pipelineExecutionId;
  String pipelineStageElementId;

  List<QLVariableInput> variableInputs;
  List<QLServiceInput> serviceInputs;
}
