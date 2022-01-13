/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.rest.RestResponse;

import software.wings.metrics.MetricType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.prometheus.PrometheusAnalysisServiceImpl;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by rsingh on 4/24/18.
 */
@Api("prometheus")
@Path("/prometheus")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class PrometheusResource implements LogAnalysisResource {
  @Inject private PrometheusAnalysisService analysisService;

  @POST
  @Path("validate-metrics")
  public RestResponse<Map<String, String>> validateMetrics(
      @QueryParam("accountId") final String accountId, @NotEmpty List<TimeSeries> timeSeriesToAnalyze) {
    return new RestResponse<>(validateTransactions(timeSeriesToAnalyze, false));
  }

  public static Map<String, String> validateTransactions(List<TimeSeries> timeSeriesToAnalyze, boolean serviceLevel) {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(timeSeriesToAnalyze)) {
      invalidFields.put("timeSeriesToAnalyze", "No metrics given to analyze.");
      return invalidFields;
    }
    Map<String, String> metricNameToType = new HashMap<>();
    final TreeBasedTable<String, MetricType, Set<String>> txnToMetricType = TreeBasedTable.create();
    timeSeriesToAnalyze.forEach(timeSeries -> {
      MetricType metricType = MetricType.valueOf(timeSeries.getMetricType());
      final String query = timeSeries.getUrl();
      if (isEmpty(query)) {
        invalidFields.put(
            "No query specified for ", "Group: " + timeSeries.getTxnName() + " Metric: " + timeSeries.getMetricName());
      }

      if (!query.contains("{") || !query.contains("{")) {
        invalidFields.put(
            "Invalid query format for group: " + timeSeries.getTxnName() + ", metric: " + timeSeries.getMetricName(),
            "Expected format example jvm_memory_max_bytes{pod_name=\"$hostName\"}");
      }

      if (!serviceLevel && isNotEmpty(query) && !query.contains(HOST_NAME_PLACE_HOLDER)
          && !query.contains(PrometheusAnalysisServiceImpl.HOST_NAME_PLACE_HOLDER)) {
        invalidFields.put(
            "Invalid query for group: " + timeSeries.getTxnName() + ", metric : " + timeSeries.getMetricName(),
            HOST_NAME_PLACE_HOLDER + " is not present in the url.");
      }

      if (metricNameToType.get(timeSeries.getMetricName()) == null) {
        metricNameToType.put(timeSeries.getMetricName(), timeSeries.getMetricType());
      } else if (!metricNameToType.get(timeSeries.getMetricName()).equals(timeSeries.getMetricType())) {
        invalidFields.put(
            "Invalid metric type for group: " + timeSeries.getTxnName() + ", metric : " + timeSeries.getMetricName(),
            timeSeries.getMetricName() + " has been configured as " + metricNameToType.get(timeSeries.getMetricName())
                + " in previous transactions. Same metric name can not have different metric types.");
      }

      if (!txnToMetricType.contains(timeSeries.getTxnName(), metricType)) {
        txnToMetricType.put(timeSeries.getTxnName(), metricType, new HashSet<>());
      }

      txnToMetricType.get(timeSeries.getTxnName(), metricType).add(timeSeries.getMetricName());
    });

    txnToMetricType.rowKeySet().forEach(txnName -> {
      final SortedMap<MetricType, Set<String>> txnRow = txnToMetricType.row(txnName);
      if (txnRow.containsKey(MetricType.ERROR) || txnRow.containsKey(MetricType.RESP_TIME)) {
        if (!txnRow.containsKey(MetricType.THROUGHPUT)) {
          invalidFields.put("Invalid metrics for group: " + txnName,
              txnName + " has error metrics "
                  + (txnRow.get(MetricType.ERROR) == null ? Collections.emptySet() : txnRow.get(MetricType.ERROR))
                  + " and/or response time metrics "
                  + (txnRow.get(MetricType.RESP_TIME) == null ? Collections.emptySet()
                                                              : txnRow.get(MetricType.RESP_TIME))
                  + " but no throughput metrics.");
        } else if (txnRow.get(MetricType.THROUGHPUT).size() > 1) {
          invalidFields.put("Invalid metrics for group: " + txnName,
              txnName + " has more than one throughput metrics " + txnRow.get(MetricType.THROUGHPUT) + " defined.");
        }
      }

      if (txnRow.containsKey(MetricType.THROUGHPUT) && txnRow.size() == 1) {
        invalidFields.put("Invalid metrics for group: " + txnName,
            txnName + " has only throughput metrics " + txnRow.get(MetricType.THROUGHPUT)
                + ". Throughput metrics is used to analyze other metrics and is not analyzed.");
      }
    });
    return invalidFields;
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param setupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @Valid PrometheusSetupTestNodeData setupTestNodeData) {
    Map<String, String> invalidFields =
        validateTransactions(setupTestNodeData.getTimeSeriesToAnalyze(), setupTestNodeData.isServiceLevel());
    if (isNotEmpty(invalidFields)) {
      throw new VerificationOperationException(
          ErrorCode.PROMETHEUS_CONFIGURATION_ERROR, "Invalid configuration, reason: " + invalidFields);
    }
    return new RestResponse<>(analysisService.getMetricsWithDataForNode(setupTestNodeData));
  }
}
