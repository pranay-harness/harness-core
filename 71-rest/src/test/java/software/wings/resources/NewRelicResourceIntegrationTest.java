package software.wings.resources;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.integration.BaseIntegrationTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.sm.states.NewRelicState;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class NewRelicResourceIntegrationTest extends BaseIntegrationTest {
  private NewRelicState.Metric requestsPerMinuteMetric;
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    requestsPerMinuteMetric = NewRelicState.Metric.builder()
                                  .metricName(NewRelicMetricValueDefinition.REQUSET_PER_MINUTE)
                                  .mlMetricType(MetricType.THROUGHPUT)
                                  .displayName("Requests per Minute")
                                  .build();
  }

  private String buildAbsoluteUrl(String path, Map<String, String> params) throws URISyntaxException {
    try {
      URIBuilder uriBuilder = new URIBuilder();
      String scheme = StringUtils.isBlank(System.getenv().get("BASE_HTTP")) ? "https" : "http";
      uriBuilder.setScheme(scheme);
      uriBuilder.setHost("localhost");
      uriBuilder.setPort(9090);
      uriBuilder.setPath(path);
      if (params != null) {
        params.forEach((name, value) -> uriBuilder.addParameter(name, value));
      }
      return uriBuilder.build().toString();
    } catch (URISyntaxException uriSyntaxException) {
      logger.error("Either the path or the baseUrl are probably incorrect.");
      throw uriSyntaxException;
    }
  }

  @Test
  /**
   * Integration test for getAllMetricNames
   * This API should return the names of all the metrics listed in the YAML
   * file corresponding to New Relic Metrics
   */
  public void getAllMetricNames() {
    Map<String, String> params = new HashMap<>();
    params.put("accountId", accountId);
    try {
      String url = buildAbsoluteUrl("/api/newrelic/metric-names", params);
      logger.info(url);
      WebTarget target = client.target(url);

      RestResponse<List<Object>> restResponse =
          getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
      List<Object> metrics = restResponse.getResource();
      assertEquals(metrics.size(), 4);

      Set<String> expectedMetricNames =
          new HashSet<>(Arrays.asList("requestsPerMinute", "averageResponseTime", "error", "apdexScore"));

      Set<String> actualMetricNames = new HashSet<>();
      for (Object metric : metrics) {
        String metricName = (String) ((LinkedHashMap) metric).get("metricName");
        actualMetricNames.add(metricName);
      }
      assertEquals(expectedMetricNames, actualMetricNames);
    } catch (URISyntaxException uriSyntaxException) {
      logger.error("Failure while building absolute URL for getting all metric names for NewRelic.");
    }
  }
}
