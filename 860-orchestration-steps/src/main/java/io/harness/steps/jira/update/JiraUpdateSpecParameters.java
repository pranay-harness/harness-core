/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.jira.update;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.jira.update.beans.TransitionTo;

import java.util.List;
import java.util.Map;
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
@TypeAlias("jiraUpdateSpecParameters")
@RecasterAlias("io.harness.steps.jira.update.JiraUpdateSpecParameters")
public class JiraUpdateSpecParameters implements SpecParameters {
  @NotNull ParameterField<String> connectorRef;
  @NotNull ParameterField<String> issueKey;

  TransitionTo transitionTo;
  Map<String, ParameterField<String>> fields;
  ParameterField<List<String>> delegateSelectors;
}
