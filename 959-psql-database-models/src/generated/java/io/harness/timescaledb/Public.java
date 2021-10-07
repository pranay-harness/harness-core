/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.Anomalies;
import io.harness.timescaledb.tables.BillingData;
import io.harness.timescaledb.tables.CeRecommendations;
import io.harness.timescaledb.tables.Environments;
import io.harness.timescaledb.tables.KubernetesUtilizationData;
import io.harness.timescaledb.tables.NgInstanceStats;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.NodePoolAggregated;
import io.harness.timescaledb.tables.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.Pipelines;
import io.harness.timescaledb.tables.PodInfo;
import io.harness.timescaledb.tables.ServiceInfraInfo;
import io.harness.timescaledb.tables.Services;
import io.harness.timescaledb.tables.WorkloadInfo;

import java.util.Arrays;
import java.util.List;
import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Public extends SchemaImpl {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public</code>
   */
  public static final Public PUBLIC = new Public();

  /**
   * The table <code>public.anomalies</code>.
   */
  public final Anomalies ANOMALIES = Anomalies.ANOMALIES;

  /**
   * The table <code>public.billing_data</code>.
   */
  public final BillingData BILLING_DATA = BillingData.BILLING_DATA;

  /**
   * The table <code>public.ce_recommendations</code>.
   */
  public final CeRecommendations CE_RECOMMENDATIONS = CeRecommendations.CE_RECOMMENDATIONS;

  /**
   * The table <code>public.environments</code>.
   */
  public final Environments ENVIRONMENTS = Environments.ENVIRONMENTS;

  /**
   * The table <code>public.kubernetes_utilization_data</code>.
   */
  public final KubernetesUtilizationData KUBERNETES_UTILIZATION_DATA =
      KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA;

  /**
   * The table <code>public.ng_instance_stats</code>.
   */
  public final NgInstanceStats NG_INSTANCE_STATS = NgInstanceStats.NG_INSTANCE_STATS;

  /**
   * The table <code>public.node_info</code>.
   */
  public final NodeInfo NODE_INFO = NodeInfo.NODE_INFO;

  /**
   * The table <code>public.node_pool_aggregated</code>.
   */
  public final NodePoolAggregated NODE_POOL_AGGREGATED = NodePoolAggregated.NODE_POOL_AGGREGATED;

  /**
   * The table <code>public.pipeline_execution_summary_cd</code>.
   */
  public final PipelineExecutionSummaryCd PIPELINE_EXECUTION_SUMMARY_CD =
      PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD;

  /**
   * The table <code>public.pipelines</code>.
   */
  public final Pipelines PIPELINES = Pipelines.PIPELINES;

  /**
   * The table <code>public.pod_info</code>.
   */
  public final PodInfo POD_INFO = PodInfo.POD_INFO;

  /**
   * The table <code>public.service_infra_info</code>.
   */
  public final ServiceInfraInfo SERVICE_INFRA_INFO = ServiceInfraInfo.SERVICE_INFRA_INFO;

  /**
   * The table <code>public.services</code>.
   */
  public final Services SERVICES = Services.SERVICES;

  /**
   * The table <code>public.workload_info</code>.
   */
  public final WorkloadInfo WORKLOAD_INFO = WorkloadInfo.WORKLOAD_INFO;

  /**
   * No further instances allowed
   */
  private Public() {
    super("public", null);
  }

  @Override
  public Catalog getCatalog() {
    return DefaultCatalog.DEFAULT_CATALOG;
  }

  @Override
  public final List<Table<?>> getTables() {
    return Arrays.<Table<?>>asList(Anomalies.ANOMALIES, BillingData.BILLING_DATA, CeRecommendations.CE_RECOMMENDATIONS,
        Environments.ENVIRONMENTS, KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
        NgInstanceStats.NG_INSTANCE_STATS, NodeInfo.NODE_INFO, NodePoolAggregated.NODE_POOL_AGGREGATED,
        PipelineExecutionSummaryCd.PIPELINE_EXECUTION_SUMMARY_CD, Pipelines.PIPELINES, PodInfo.POD_INFO,
        ServiceInfraInfo.SERVICE_INFRA_INFO, Services.SERVICES, WorkloadInfo.WORKLOAD_INFO);
  }
}
