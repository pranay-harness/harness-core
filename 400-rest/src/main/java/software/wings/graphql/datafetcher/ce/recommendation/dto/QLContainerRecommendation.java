/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.recommendation.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLContainerRecommendation implements QLObject {
  String containerName;
  QLResourceRequirement current;
  QLResourceRequirement burstable;
  QLResourceRequirement guaranteed;
  QLResourceRequirement recommended;
  //  requiredPercentiles 50, 80, 90, 95, 99
  QLResourceRequirement p50;
  QLResourceRequirement p80;
  QLResourceRequirement p90;
  QLResourceRequirement p95;
  QLResourceRequirement p99;
  int numDays;
  int totalSamplesCount;
}
