/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("k8sStepPassThroughData")
@RecasterAlias("io.harness.cdng.k8s.K8sStepPassThroughData")
public class K8sStepPassThroughData implements PassThroughData {
  ManifestOutcome k8sManifestOutcome;
  List<ValuesManifestOutcome> valuesManifestOutcomes;
  List<OpenshiftParamManifestOutcome> openshiftParamManifestOutcomes;
  InfrastructureOutcome infrastructure;
  String helmValuesFileContent;
}
