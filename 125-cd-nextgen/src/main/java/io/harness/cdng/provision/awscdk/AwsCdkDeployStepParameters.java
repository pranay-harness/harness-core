/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.awscdk;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.container.ContainerResource;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("awsCdkDeployStepParameters")
@RecasterAlias("io.harness.cdng.provision.awscdk.AwsCdkDeployStepParameters")
public class AwsCdkDeployStepParameters extends AwsCdkBaseStepInfo implements SpecParameters, StepParameters {
  ParameterField<List<String>> stackNames;

  ParameterField<String> provisionerIdentifier;

  ParameterField<Map<String, String>> parameters;

  @Builder(builderMethodName = "infoBuilder")
  public AwsCdkDeployStepParameters(ParameterField<List<TaskSelectorYaml>> delegateSelectors,
      ParameterField<String> image, ParameterField<String> connectorRef, ContainerResource resources,
      ParameterField<Map<String, String>> envVariables, ParameterField<Boolean> privileged,
      ParameterField<Integer> runAsUser, ParameterField<ImagePullPolicy> imagePullPolicy,
      ParameterField<List<String>> commandOptions, ParameterField<String> appPath,
      ParameterField<List<String>> stackNames, ParameterField<String> provisionerIdentifier,
      ParameterField<Map<String, String>> parameters) {
    super(delegateSelectors, image, connectorRef, resources, envVariables, privileged, runAsUser, imagePullPolicy,
        commandOptions, appPath);
    this.stackNames = stackNames;
    this.provisionerIdentifier = provisionerIdentifier;
    this.parameters = parameters;
  }
}
