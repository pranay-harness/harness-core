/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

import software.wings.resources.DashboardStatisticsResource;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InstanceStatsUtils {
  /**
   * Find 95th percentile of usage in last 30 days.
   * This will serve as 'average' usage in last 30 days (the word 'average' is not used mathematically here)
   */
  public static double actualUsage(String accountId, InstanceStatService instanceStatService) {
    Instant now = Instant.now();
    Instant from = now.minus(30, ChronoUnit.DAYS);
    return instanceStatService.percentile(accountId, from, now, DashboardStatisticsResource.DEFAULT_PERCENTILE);
  }

  public static double currentActualUsage(String accountId, InstanceStatService instanceStatService) {
    return instanceStatService.currentCount(accountId);
  }
}
