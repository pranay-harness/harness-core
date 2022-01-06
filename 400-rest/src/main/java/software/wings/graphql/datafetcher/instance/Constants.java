/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.instance;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class Constants {
  private Constants() {}

  public static final String PARTIAL_AGGREGATION_INTERVAL = "PARTIAL_AGGREGATION";
  public static final String COMPLETE_AGGREGATION_INTERVAL = "COMPLETE_AGGREGATION";
  public static final String MONTH_TIMESTAMP_COL_NAME = "MONTHTIMESTAMP";
  public static final String WEEK_TIMESTAMP_COL_NAME = "WEEKTIMESTAMP";
  public static final String REPORTED_AT_COL_NAME = "REPORTEDAT";
  public static final String INSTANCE_STATS_HOUR_TABLE_NAME = "INSTANCE_STATS_HOUR";
  public static final String INSTANCE_STATS_DAY_TABLE_NAME = "INSTANCE_STATS_DAY";
  public static final String INSTANCE_STATS_TABLE_NAME = "INSTANCE_STATS";
}
