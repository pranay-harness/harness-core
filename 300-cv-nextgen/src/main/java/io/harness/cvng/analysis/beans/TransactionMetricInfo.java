/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.beans;

import io.harness.cvng.beans.DataSourceType;

import java.util.Map;
import java.util.SortedSet;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Data
@Builder
public class TransactionMetricInfo {
  private TransactionMetric transactionMetric;
  private String connectorName;
  private DataSourceType dataSourceType;
  private SortedSet<DeploymentTimeSeriesAnalysisDTO.HostData> nodes;
  private Map<Risk, Integer> nodeCountByRiskStatusMap;

  @Value
  @Builder
  @EqualsAndHashCode(of = {"transactionName", "metricName"})
  public static class TransactionMetric {
    String transactionName;
    String metricName;
    private Double score; // is this score is needed in the UI?
    private Risk risk;
  }
}
