/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("jiraApprovalSpecParameters")
@RecasterAlias("io.harness.steps.approval.step.jira.JiraApprovalSpecParameters")
public class JiraApprovalSpecParameters implements SpecParameters {
  @NotNull ParameterField<String> connectorRef;
  @NotNull ParameterField<String> issueKey;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  CriteriaSpecWrapper rejectionCriteria;
  ParameterField<List<String>> delegateSelectors;
}
