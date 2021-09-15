/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@FieldNameConstants(innerTypeName = "ApprovalStateParamsKeys")
public class ApprovalStateParams {
  @Getter @Setter private JiraApprovalParams jiraApprovalParams;
  @Getter @Setter private ShellScriptApprovalParams shellScriptApprovalParams;
  @Getter @Setter private ServiceNowApprovalParams serviceNowApprovalParams;
}
