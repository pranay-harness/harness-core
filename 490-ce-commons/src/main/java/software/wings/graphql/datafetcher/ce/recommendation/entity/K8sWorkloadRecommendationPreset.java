/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sWorkloadRecommendationPreset {
  // All percentiles are doubles in range (0, 1)

  // requests
  double cpuRequest;
  double memoryRequest;

  // limits (set <= 0 to omit)
  double cpuLimit;
  double memoryLimit;

  // Fraction to add on top
  double safetyMargin;
}
