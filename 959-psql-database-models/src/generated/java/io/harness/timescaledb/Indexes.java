/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.Anomalies;
import io.harness.timescaledb.tables.BillingData;
import io.harness.timescaledb.tables.Environments;
import io.harness.timescaledb.tables.KubernetesUtilizationData;
import io.harness.timescaledb.tables.NodeInfo;
import io.harness.timescaledb.tables.Pipelines;
import io.harness.timescaledb.tables.PodInfo;
import io.harness.timescaledb.tables.Services;

import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;

/**
 * A class modelling indexes of tables in public.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Indexes {
  // -------------------------------------------------------------------------
  // INDEX definitions
  // -------------------------------------------------------------------------

  public static final Index ANOMALIES_ANOMALYTIME_IDX = Internal.createIndex(DSL.name("anomalies_anomalytime_idx"),
      Anomalies.ANOMALIES, new OrderField[] {Anomalies.ANOMALIES.ANOMALYTIME.desc()}, false);
  public static final Index ANOMALIES_PKEY = Internal.createIndex(DSL.name("anomalies_pkey"), Anomalies.ANOMALIES,
      new OrderField[] {Anomalies.ANOMALIES.ID, Anomalies.ANOMALIES.ANOMALYTIME}, true);
  public static final Index ANOMALY_ACCOUNTID_INDEX =
      Internal.createIndex(DSL.name("anomaly_accountid_index"), Anomalies.ANOMALIES,
          new OrderField[] {Anomalies.ANOMALIES.ACCOUNTID, Anomalies.ANOMALIES.ANOMALYTIME.desc()}, false);
  public static final Index BILLING_DATA_ACCOUNTID_INDEX =
      Internal.createIndex(DSL.name("billing_data_accountid_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.STARTTIME.desc()}, false);
  public static final Index BILLING_DATA_APPID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_appid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.APPID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLOUDPROVIDERID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_cloudproviderid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLOUDPROVIDERID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLOUDSERVICENAME_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_cloudservicename_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.CLOUDSERVICENAME, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_CLUSTERID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_clusterid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_LAUNCHTYPE_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_launchtype_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.LAUNCHTYPE, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_NAMESPACE_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_namespace_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.NAMESPACE, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_NAMESPACE_WITHOUT_CLUSTER_INDEX =
      Internal.createIndex(DSL.name("billing_data_namespace_without_cluster_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.NAMESPACE,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_STARTTIME_IDX = Internal.createIndex(DSL.name("billing_data_starttime_idx"),
      BillingData.BILLING_DATA, new OrderField[] {BillingData.BILLING_DATA.STARTTIME.desc()}, false);
  public static final Index BILLING_DATA_TASKID_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_taskid_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.TASKID, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_UNIQUE_INDEX =
      Internal.createIndex(DSL.name("billing_data_unique_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.SETTINGID,
              BillingData.BILLING_DATA.CLUSTERID, BillingData.BILLING_DATA.INSTANCEID,
              BillingData.BILLING_DATA.INSTANCETYPE, BillingData.BILLING_DATA.STARTTIME.desc()},
          true);
  public static final Index BILLING_DATA_WORKLOADNAME_COMPOSITE_INDEX =
      Internal.createIndex(DSL.name("billing_data_workloadname_composite_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.CLUSTERID,
              BillingData.BILLING_DATA.WORKLOADNAME, BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index BILLING_DATA_WORKLOADNAME_WITHOUT_CLUSTER_INDEX =
      Internal.createIndex(DSL.name("billing_data_workloadname_without_cluster_index"), BillingData.BILLING_DATA,
          new OrderField[] {BillingData.BILLING_DATA.ACCOUNTID, BillingData.BILLING_DATA.WORKLOADNAME,
              BillingData.BILLING_DATA.STARTTIME.desc()},
          false);
  public static final Index ENVIRONMENTS_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("environments_account_id_created_at_idx"), Environments.ENVIRONMENTS,
          new OrderField[] {Environments.ENVIRONMENTS.ACCOUNT_ID, Environments.ENVIRONMENTS.CREATED_AT}, false);
  public static final Index KUBERNETES_UTILIZATION_DATA_ACCID_CLUSTERID_ACINSTANCEID =
      Internal.createIndex(DSL.name("kubernetes_utilization_data_accid_clusterid_acinstanceid"),
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
          new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACCOUNTID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.CLUSTERID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACTUALINSTANCEID,
              KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
          false);
  public static final Index KUBERNETES_UTILIZATION_DATA_INSTANCEID_INDEX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_instanceid_index"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCEID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
      false);
  public static final Index KUBERNETES_UTILIZATION_DATA_STARTTIME_IDX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_starttime_idx"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()}, false);
  public static final Index KUBERNETES_UTILIZATION_DATA_UNIQUE_INDEX = Internal.createIndex(
      DSL.name("kubernetes_utilization_data_unique_index"), KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA,
      new OrderField[] {KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.ACCOUNTID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.SETTINGID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.CLUSTERID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCEID,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.INSTANCETYPE,
          KubernetesUtilizationData.KUBERNETES_UTILIZATION_DATA.STARTTIME.desc()},
      true);
  public static final Index NODE_INFO_ACCID_CLUSTERID_POOLNAME = Internal.createIndex(
      DSL.name("node_info_accid_clusterid_poolname"), NodeInfo.NODE_INFO,
      new OrderField[] {NodeInfo.NODE_INFO.ACCOUNTID, NodeInfo.NODE_INFO.CLUSTERID, NodeInfo.NODE_INFO.NODEPOOLNAME},
      false);
  public static final Index PIPELINES_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("pipelines_account_id_created_at_idx"), Pipelines.PIPELINES,
          new OrderField[] {Pipelines.PIPELINES.ACCOUNT_ID, Pipelines.PIPELINES.CREATED_AT}, false);
  public static final Index POD_INFO_STARTTIME_IDX = Internal.createIndex(DSL.name("pod_info_starttime_idx"),
      PodInfo.POD_INFO, new OrderField[] {PodInfo.POD_INFO.STARTTIME.desc()}, false);
  public static final Index POD_INFO_STARTTIME_UNIQUE_RECORD_INDEX =
      Internal.createIndex(DSL.name("pod_info_starttime_unique_record_index"), PodInfo.POD_INFO,
          new OrderField[] {PodInfo.POD_INFO.ACCOUNTID, PodInfo.POD_INFO.CLUSTERID, PodInfo.POD_INFO.INSTANCEID,
              PodInfo.POD_INFO.STARTTIME.desc()},
          true);
  public static final Index SERVICES_ACCOUNT_ID_CREATED_AT_IDX =
      Internal.createIndex(DSL.name("services_account_id_created_at_idx"), Services.SERVICES,
          new OrderField[] {Services.SERVICES.ACCOUNT_ID, Services.SERVICES.CREATED_AT}, false);
}
