/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDAbstractStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.K8sDryRunManifestStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_DRY_RUN_MANIFEST)
@SimpleVisitorHelper(helperClass = K8sDryRunManifestStepInfoVisitorHelper.class)
@TypeAlias("K8sDryRunManifestStepInfo")
@RecasterAlias("io.harness.cdng.k8s.K8sDryRunManifestStepInfo")
public class K8sDryRunManifestStepInfo extends K8sDryRunManifestBaseStepInfo implements CDAbstractStepInfo, Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @JsonIgnore private String name;
  @JsonIgnore private String identifier;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public K8sDryRunManifestStepInfo(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String canaryStepFqn, String name, String identifier) {
    super(delegateSelectors, canaryStepFqn);
    this.name = name;
    this.identifier = identifier;
  }

  public K8sDryRunManifestStepInfo(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public StepType getStepType() {
    return K8sDryRunManifestStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return K8sDryRunManifestStepParameters.infoBuilder().delegateSelectors(delegateSelectors).build();
  }

  @Override
  public ParameterField<List<TaskSelectorYaml>> fetchDelegateSelectors() {
    return getDelegateSelectors();
  }
}