/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.rule.OwnerRule.UNKNOWN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class NewRelicResourceIntegrationTest extends IntegrationTestBase {
  private NewRelicState.Metric requestsPerMinuteMetric;
  @Override
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
        params.forEach(uriBuilder::addParameter);
      }
      return uriBuilder.build().toString();
    } catch (URISyntaxException uriSyntaxException) {
      log.error("Either the path or the baseUrl are probably incorrect.");
      throw uriSyntaxException;
    }
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
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
      log.info(url);
      WebTarget target = client.target(url);

      RestResponse<List<Object>> restResponse =
          getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<Object>>>() {});
      List<Object> metrics = restResponse.getResource();
      assertThat(4).isEqualTo(metrics.size());

      Set<String> expectedMetricNames =
          new HashSet<>(Arrays.asList("requestsPerMinute", "averageResponseTime", "error", "apdexScore"));

      Set<String> actualMetricNames = new HashSet<>();
      for (Object metric : metrics) {
        String metricName = (String) ((LinkedHashMap) metric).get("metricName");
        actualMetricNames.add(metricName);
      }
      assertThat(actualMetricNames).isEqualTo(expectedMetricNames);
    } catch (URISyntaxException uriSyntaxException) {
      log.error("Failure while building absolute URL for getting all metric names for NewRelic.");
    }
  }
}
