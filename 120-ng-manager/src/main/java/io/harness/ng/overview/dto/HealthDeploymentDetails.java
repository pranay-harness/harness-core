/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.overview.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HealthDeploymentDetails {
  long total;
  long currentSuccess;
  long currentFailed;
  long currentActive;
  long previousSuccess;
  long previousFailed;
  long previousDeployment;
  long previousActive;
  long production;
  long nonProduction;
  List<DeploymentDateAndCount> totalDateAndCount;
  List<DeploymentDateAndCount> successDateAndCount;
  List<DeploymentDateAndCount> failedDateAndCount;
  List<DeploymentDateAndCount> activeDateAndCount;
}
