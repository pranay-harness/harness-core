/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.integrationstage;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;

import java.util.List;

public interface InitializeStepInfoBuilder {
  BuildJobEnvInfo getInitializeStepInfoBuilder(StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs,
      List<ExecutionWrapperConfig> steps, String accountId);
}
