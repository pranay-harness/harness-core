/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivityDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("HarnessApprovalOutcome")
@TypeAlias("harnessApprovalOutcome")
@RecasterAlias("io.harness.steps.approval.step.harness.HarnessApprovalOutcome")
public class HarnessApprovalOutcome implements Outcome {
  List<HarnessApprovalActivityDTO> approvalActivities;
  Map<String, String> approverInputs;
}
