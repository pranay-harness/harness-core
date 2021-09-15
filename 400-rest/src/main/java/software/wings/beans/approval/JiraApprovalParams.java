/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
public class JiraApprovalParams {
  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter private String approvalField;
  @Getter @Setter private String approvalValue;
  @Getter @Setter private String rejectionField;
  @Getter @Setter private String rejectionValue;
  @Getter @Setter private String issueId;
  @Getter @Setter private String project;
}
