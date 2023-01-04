/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.tas;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.TasAppResizeStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = TasAppResizeStepInfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.TAS_APP_RESIZE)
@TypeAlias("TasAppResizeStepInfo")
@RecasterAlias("io.harness.cdng.tas.TasAppResizeStepInfo")
public class TasAppResizeStepInfo extends TasAppResizeBaseStepInfo implements CDStepInfo, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public TasAppResizeStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      TasInstanceSelectionWrapper newAppInstances, TasInstanceSelectionWrapper oldAppInstances, String tasBGAppSetupFqn,
      String tasBasicAppSetupFqn, String tasCanaryAppSetupFqn) {
    super(newAppInstances, oldAppInstances, delegateSelectors, tasBGAppSetupFqn, tasBasicAppSetupFqn,
        tasCanaryAppSetupFqn);
  }
  @Override
  public StepType getStepType() {
    return TasAppResizeStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return TasAppResizeStepParameters.infoBuilder()
        .newAppInstances(newAppInstances)
        .oldAppInstances(oldAppInstances)
        .tasBGAppSetupFqn(tasBGSetupFqn)
        .tasBasicAppSetupFqn(tasBasicSetupFqn)
        .tasCanaryAppSetupFqn(tasCanarySetupFqn)
        .delegateSelectors(this.getDelegateSelectors())
        .build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}