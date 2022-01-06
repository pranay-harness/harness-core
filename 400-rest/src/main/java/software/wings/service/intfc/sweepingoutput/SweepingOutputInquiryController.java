/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import software.wings.api.PhaseElement;
import software.wings.sm.StateExecutionInstance;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@UtilityClass
@Slf4j
public class SweepingOutputInquiryController {
  public static SweepingOutputInquiry obtainFromStateExecutionInstance(
      @NotNull StateExecutionInstance stateExecutionInstance, @Nullable @Trimmed String namePrefix) {
    String name = stateExecutionInstance.getDisplayName().trim();
    if (namePrefix != null) {
      name = namePrefix + name;
    }
    return SweepingOutputInquiry.builder()
        .appId(stateExecutionInstance.getAppId())
        .name(name)
        .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
        .stateExecutionId(stateExecutionInstance.getUuid())
        .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
        .build();
  }

  public static SweepingOutputInquiry obtainFromStateExecutionInstanceWithoutName(
      @NotNull StateExecutionInstance stateExecutionInstance) {
    return SweepingOutputInquiry.builder()
        .appId(stateExecutionInstance.getAppId())
        .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
        .stateExecutionId(stateExecutionInstance.getUuid())
        .phaseExecutionId(getPhaseExecutionId(stateExecutionInstance))
        .build();
  }

  @Nullable
  private static String getPhaseExecutionId(@NotNull StateExecutionInstance stateExecutionInstance) {
    PhaseElement phaseElement = stateExecutionInstance.fetchPhaseElement();
    return phaseElement == null
        ? null
        : stateExecutionInstance.getExecutionUuid() + phaseElement.getUuid() + phaseElement.getPhaseName();
  }
}
