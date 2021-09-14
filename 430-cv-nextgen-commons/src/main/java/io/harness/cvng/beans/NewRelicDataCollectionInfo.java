/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.cvng.newrelic.NewRelicUtils;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class NewRelicDataCollectionInfo extends TimeSeriesDataCollectionInfo<NewRelicConnectorDTO> {
  private static final String QUERIES_KEY = "queries";
  private static final String JSON_PATH_KEY = "jsonPaths";
  private static final String METRIC_NAMES_KEY = "metricNames";

  private String applicationName;
  private long applicationId;
  private MetricPackDTO metricPack;

  @Override
  public Map<String, Object> getDslEnvVariables(NewRelicConnectorDTO newRelicConnectorDTO) {
    Map<String, Object> dslEnvVariables = new HashMap<>();
    dslEnvVariables.put("appName", getApplicationName());
    dslEnvVariables.put("appId", getApplicationId());

    List<String> listOfQueries =
        metricPack.getMetrics().stream().map(MetricPackDTO.MetricDefinitionDTO::getPath).collect(Collectors.toList());
    List<String> listOfJsonPaths = metricPack.getMetrics()
                                       .stream()
                                       .map(MetricPackDTO.MetricDefinitionDTO::getResponseJsonPath)
                                       .collect(Collectors.toList());
    List<String> listOfMetricNames =
        metricPack.getMetrics().stream().map(MetricPackDTO.MetricDefinitionDTO::getName).collect(Collectors.toList());

    Preconditions.checkState(listOfQueries.size() == listOfMetricNames.size());
    Preconditions.checkState(listOfQueries.size() == listOfJsonPaths.size());

    boolean nullPath = listOfJsonPaths.stream().anyMatch(path -> path == null);
    Preconditions.checkState(!nullPath, "There can't be any null json paths");

    dslEnvVariables.put(QUERIES_KEY, listOfQueries);
    dslEnvVariables.put(JSON_PATH_KEY, listOfJsonPaths);
    dslEnvVariables.put(METRIC_NAMES_KEY, listOfMetricNames);

    dslEnvVariables.put("collectHostData", Boolean.toString(this.isCollectHostData()));

    return dslEnvVariables;
  }

  @Override
  public String getBaseUrl(NewRelicConnectorDTO newRelicConnectorDTO) {
    return NewRelicUtils.getBaseUrl(newRelicConnectorDTO);
  }

  @Override
  public Map<String, String> collectionHeaders(NewRelicConnectorDTO newRelicConnectorDTO) {
    return NewRelicUtils.collectionHeaders(newRelicConnectorDTO);
  }

  @Override
  public Map<String, String> collectionParams(NewRelicConnectorDTO newRelicConnectorDTO) {
    return Collections.emptyMap();
  }
}
