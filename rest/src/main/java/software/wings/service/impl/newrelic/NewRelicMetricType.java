package software.wings.service.impl.newrelic;

import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Created by rsingh on 8/30/17.
 */
@AllArgsConstructor
public enum NewRelicMetricType {
  APDEX("Apdex", NewRelicMetricValuesCollectionDefinition.apdexValues),
  WEB_TRANSACTION("WebTransaction", NewRelicMetricValuesCollectionDefinition.webTransactionsValues),
  ERRORS("Errors/all", NewRelicMetricValuesCollectionDefinition.allErrors);

  private final String metricName;
  private final List<String> valuesToCollect;

  public String getMetricName() {
    return metricName;
  }

  public List<String> getValuesToCollect() {
    return valuesToCollect;
  }
}
