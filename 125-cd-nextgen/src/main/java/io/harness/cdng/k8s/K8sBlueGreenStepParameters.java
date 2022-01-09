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
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@TypeAlias("K8sBlueGreenStepParameters")
@RecasterAlias("io.harness.cdng.k8s.K8sBlueGreenStepParameters")
public class K8sBlueGreenStepParameters extends K8sBlueGreenBaseStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sBlueGreenStepParameters(
      ParameterField<Boolean> skipDryRun, ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
    super(skipDryRun, delegateSelectors);
  }
}
