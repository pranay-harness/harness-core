/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public class PhaseStepExecutionSummary extends StepExecutionSummary {
  private List<StepExecutionSummary> stepExecutionSummaryList = new ArrayList<>();

  public List<StepExecutionSummary> getStepExecutionSummaryList() {
    return stepExecutionSummaryList;
  }

  public void setStepExecutionSummaryList(List<StepExecutionSummary> stepExecutionSummaryList) {
    this.stepExecutionSummaryList = stepExecutionSummaryList;
  }
}
