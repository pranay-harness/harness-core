/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.FailureStrategy;
import software.wings.beans.NotificationRule;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.beans.VariableYaml;
import software.wings.beans.WorkflowPhase;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("MULTI_SERVICE")
@TargetModule(HarnessModule._957_CG_BEANS)
public class MultiServiceWorkflowYaml extends WorkflowYaml {
  @Builder
  public MultiServiceWorkflowYaml(String type, String harnessApiVersion, String description,
      List<Yaml> templateExpressions, String envName, boolean templatized, List<StepYaml> preDeploymentSteps,
      List<WorkflowPhase.Yaml> phases, List<WorkflowPhase.Yaml> rollbackPhases, List<StepYaml> postDeploymentSteps,
      List<NotificationRule.Yaml> notificationRules, List<FailureStrategy.Yaml> failureStrategies,
      List<VariableYaml> userVariables, String concurrencyStrategy) {
    super(type, harnessApiVersion, description, templateExpressions, envName, templatized, preDeploymentSteps, phases,
        rollbackPhases, postDeploymentSteps, notificationRules, failureStrategies, userVariables, concurrencyStrategy,
        null, null, null, null);
  }
}
