/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.stats;

import io.harness.beans.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeploymentStatistics extends WingsStatistics {
  private Map<EnvironmentType, AggregatedDayStats> statsMap = new HashMap<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AggregatedDayStats {
    private int totalCount;
    private int failedCount;
    private int instancesCount;
    private List<DayStat> daysStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayStat {
      private int totalCount;
      private int failedCount;
      private int instancesCount;
      private Long date;
    }
  }
}
