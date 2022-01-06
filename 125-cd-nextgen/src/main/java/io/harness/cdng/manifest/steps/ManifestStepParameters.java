/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("manifestStepParameters")
@RecasterAlias("io.harness.cdng.manifest.steps.ManifestStepParameters")
public class ManifestStepParameters implements StepParameters {
  String identifier;
  String type;
  ManifestAttributes spec;
  @Singular List<ManifestAttributes> overrideSets;
  ManifestAttributes stageOverride;
  int order;
}
