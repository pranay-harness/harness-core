/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.Anomalies;
import io.harness.timescaledb.tables.BillingData;
import io.harness.timescaledb.tables.CeRecommendations;
import io.harness.timescaledb.tables.KubernetesUtilizationData;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.NodePoolAggregated;
import io.harness.timescaledb.tables.PodInfo;
import io.harness.timescaledb.tables.WorkloadInfo;

/**
 * Convenience access to all tables in public.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Tables {
  /**
   * The table <code>public.anomalies</code>.
   */
  public static final Anomalies ANOMALIES = Anomalies.ANOMALIES;

  /**
   * The table <code>public.billing_data</code>.
   */
  public static final BillingData BILLING_DATA = BillingData.BILLING_DATA;

  /**
   * The table <code>public.ce_recommendations</code>.
   */
  public static final CeRecommendations CE_RECOMMENDATIONS = CeRecommendations.CE_RECOMMENDATIONS;

  /**
   * The table <code>public.kubernetes_utilization_data</code>.
   */
  public static final KubernetesUtilizationData KUBERNETES_UTILIZATION_DATA =
      KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA;

  /**
   * The table <code>public.node_info</code>.
   */
  public static final NodeInfo NODE_INFO = NodeInfo.NODE_INFO;

  /**
   * The table <code>public.node_pool_aggregated</code>.
   */
  public static final NodePoolAggregated NODE_POOL_AGGREGATED = NodePoolAggregated.NODE_POOL_AGGREGATED;

  /**
   * The table <code>public.pod_info</code>.
   */
  public static final PodInfo POD_INFO = PodInfo.POD_INFO;

  /**
   * The table <code>public.workload_info</code>.
   */
  public static final WorkloadInfo WORKLOAD_INFO = WorkloadInfo.WORKLOAD_INFO;
}
