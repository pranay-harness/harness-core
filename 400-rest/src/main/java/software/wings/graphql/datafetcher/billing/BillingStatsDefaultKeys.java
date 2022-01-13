/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class BillingStatsDefaultKeys {
  private BillingStatsDefaultKeys() {}

  public static final String ENTITYID = "Default_EntityId";
  public static final String TYPE = "Default_type";
  public static final String NAME = "Total";
  public static final Double TOTALCOST = 0.0;
  public static final Double IDLECOST = 0.0;
  public static final Double NETWORKCOST = 0.0;
  public static final Double SYSTEMCOST = 0.0;
  public static final Double CPUIDLECOST = 0.0;
  public static final Double MEMORYIDLECOST = 0.0;
  public static final Double COSTTREND = 0.0;
  public static final String TRENDTYPE = "Increasing";
  public static final String REGION = "-";
  public static final String LAUNCHTYPE = "-";
  public static final String CLOUDSERVICENAME = "Total";
  public static final String WORKLOADNAME = "Total";
  public static final String WORKLOADTYPE = "Default_WorkloadType";
  public static final String NAMESPACE = "Total";
  public static final String CLUSTERTYPE = "Default_ClusterType";
  public static final String CLUSTERID = "Default_ClusterId";
  public static final String CLUSTERNAME = "Default_ClusterName";
  public static final String TASKID = "Total";
  public static final int TOTALWORKLOADS = 0;
  public static final int TOTALNAMESPACES = 0;
  public static final double MAXCPUUTILIZATION = 1.0;
  public static final double MAXMEMORYUTILIZATION = 1.0;
  public static final double AVGCPUUTILIZATION = 1.0;
  public static final double AVGMEMORYUTILIZATION = 1.0;
  public static final long MINSTARTTIME = 0L;
  public static final long MAXSTARTTIME = 0L;
  public static final String ENVIRONMENT = "Default_Environment";
  public static final String CLOUDPROVIDER = "Default_CloudProvider";
  public static final Double UNALLOCATEDCOST = 0.0;
  public static final String TOKEN = ":";
  public static final String LABEL = "Total";
  public static final String OTHERS = "Others";
  public static final String DEFAULT_LABEL = "Label not present";
  public static final String DEFAULT_TAG = "Tag not present";
  public static final Integer DEFAULT_LIMIT = Integer.MAX_VALUE - 1;
  public static final int EFFICIENCY_SCORE = -1;
  public static final int EFFICIENCY_SCORE_TREND = -1;
  public static final String INSTANCETYPE = "unknown";
}
