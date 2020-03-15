package software.wings.service.impl.prometheus;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;
import software.wings.sm.states.APMVerificationState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrometheusAnalysisServiceImplTest extends WingsBaseTest {
  @Inject private PrometheusAnalysisService prometheusAnalysisService;

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void test_apmMetricEndPointsFetchInfo_whenNoTimeSeries() {
    assertThat(prometheusAnalysisService.apmMetricEndPointsFetchInfo(Lists.newArrayList())).isEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void test_apmMetricEndPointsFetchInfo() {
    int numOfTimeSeries = 5;
    List<TimeSeries> timeSeriesList = new ArrayList<>();
    for (int i = 0; i < numOfTimeSeries; i++) {
      timeSeriesList.add(TimeSeries.builder()
                             .txnName("txn-" + i)
                             .url("url-" + i)
                             .metricName("metric-" + i)
                             .metricType(MetricType.INFRA.name())
                             .build());
    }
    final Map<String, List<APMMetricInfo>> apmMetricEndPointsFetchInfo =
        prometheusAnalysisService.apmMetricEndPointsFetchInfo(timeSeriesList);
    assertThat(apmMetricEndPointsFetchInfo.size()).isEqualTo(numOfTimeSeries);

    for (int i = 0; i < numOfTimeSeries; i++) {
      final List<APMMetricInfo> metricInfos = apmMetricEndPointsFetchInfo.get(
          "/api/v1/query_range?start=${start_time_seconds}&end=${end_time_seconds}&step=60s&query=url-" + i);
      assertThat(metricInfos.size()).isEqualTo(1);

      final int index = i;
      metricInfos.forEach(apmMetricInfo -> {
        assertThat(apmMetricInfo.getMetricName()).isEqualTo("metric-" + index);
        assertThat(apmMetricInfo.getMetricType()).isEqualTo(MetricType.INFRA);
        assertThat(apmMetricInfo.getMethod()).isEqualTo(APMVerificationState.Method.GET);

        final Map<String, APMMetricInfo.ResponseMapper> responseMappers = apmMetricInfo.getResponseMappers();
        assertThat(responseMappers.size()).isEqualTo(3);

        assertThat(responseMappers.containsKey("txnName"));
        APMMetricInfo.ResponseMapper responseMapper = responseMappers.get("txnName");
        assertThat(responseMapper.getFieldName()).isEqualTo("txnName");
        assertThat(responseMapper.getFieldValue()).isEqualTo("txn-" + index);

        assertThat(responseMappers.containsKey("timestamp"));
        responseMapper = responseMappers.get("timestamp");
        assertThat(responseMapper.getFieldName()).isEqualTo("timestamp");
        assertThat(responseMapper.getJsonPath()).isNotEmpty();
        assertThat(responseMapper.getFieldValue()).isNull();

        assertThat(responseMappers.containsKey("value"));
        responseMapper = responseMappers.get("value");
        assertThat(responseMapper.getFieldName()).isEqualTo("value");
        assertThat(responseMapper.getJsonPath()).isNotEmpty();
        assertThat(responseMapper.getFieldValue()).isNull();
      });
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void test_renderFetchQueries() {
    List<TimeSeries> timeSeriesList = new ArrayList<>();
    timeSeriesList.add(TimeSeries.builder()
                           .url("container_memory_usage_bytes{container_name=\"POD\", pod_name=\"$hostName\"}")
                           .metricType(MetricType.INFRA.name())
                           .txnName(generateUuid())
                           .metricName(generateUuid())
                           .build());

    timeSeriesList.add(
        TimeSeries.builder()
            .url(
                "/api/v1/query_range?start=$startTime&end=$endTime&step=60s&query=container_memory_usage_bytes{container_name=\"POD\", pod_name=\"${host}\"}")
            .metricType(MetricType.INFRA.name())
            .txnName(generateUuid())
            .metricName(generateUuid())
            .build());

    final Map<String, List<APMMetricInfo>> renderFetchQueries =
        prometheusAnalysisService.apmMetricEndPointsFetchInfo(timeSeriesList);

    renderFetchQueries.forEach(
        (url, apmMetricInfos)
            -> assertThat(url).isEqualTo(
                "/api/v1/query_range?start=${start_time_seconds}&end=${end_time_seconds}&step=60s&query=container_memory_usage_bytes{container_name=\"POD\", pod_name=\"${host}\"}"));
  }
}