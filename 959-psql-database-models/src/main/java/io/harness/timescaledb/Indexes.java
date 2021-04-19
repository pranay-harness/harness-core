/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.KubernetesUtilizationData;

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
}
