/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.appdynamics;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.verification.MetricCVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@TargetModule(HarnessModule._955_CG_YAML)
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public final class AppDynamicsCVConfigurationYaml extends MetricCVConfigurationYaml {
  private String appDynamicsApplicationName;
  private String tierName;
}
