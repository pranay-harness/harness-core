package software.wings.service.impl.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;

import com.google.inject.Inject;

import io.fabric8.utils.Lists;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CVConfigurationServiceImplTest extends WingsBaseTest {
  @Inject private CVConfigurationService cvConfigurationService;

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateAppDynamics() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.APP_DYNAMICS, null);
    Map<String, TimeSeriesMetricDefinition> expectedDefinitions =
        NewRelicMetricValueDefinition.APP_DYNAMICS_24X7_VALUES_TO_ANALYZE;
    assertThat(expectedDefinitions).isEqualTo(actualDefinitions);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateNewRelic() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.NEW_RELIC, null);
    assertThat(actualDefinitions).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateDynaTrace() {
    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.DYNA_TRACE, null);
    assertThat(actualDefinitions).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplatePrometheus() {
    List<TimeSeries> timeSeriesToAnalyze = new ArrayList<>();

    timeSeriesToAnalyze.add(
        TimeSeries.builder().metricName("cpu").metricType(MetricType.INFRA.name()).txnName("cpu").url("url1").build());

    timeSeriesToAnalyze.add(TimeSeries.builder()
                                .metricName("error")
                                .metricType(MetricType.ERROR.name())
                                .txnName("error")
                                .url("url2")
                                .build());

    timeSeriesToAnalyze.add(TimeSeries.builder()
                                .metricName("throughput")
                                .metricType(MetricType.THROUGHPUT.name())
                                .txnName("throughput")
                                .url("url3")
                                .build());

    PrometheusCVServiceConfiguration cvServiceConfiguration =
        PrometheusCVServiceConfiguration.builder().timeSeriesToAnalyze(timeSeriesToAnalyze).build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.PROMETHEUS, cvServiceConfiguration);
    assertThat(actualDefinitions).hasSize(3);

    timeSeriesToAnalyze.forEach(timeSeries -> {
      TimeSeriesMetricDefinition metricDefinition = actualDefinitions.get(timeSeries.getMetricName());
      assertThat(metricDefinition).isNotNull();
      assertThat(metricDefinition.getMetricName()).isEqualTo(timeSeries.getMetricName());
      assertThat(metricDefinition.getMetricType().name()).isEqualTo(timeSeries.getMetricType());
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplateDatadog() {
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("filter1", "docker.cpu.usage, docker.mem.rss");
    dockerMetrics.put("filter2", "docker.cpu.throttled");

    Map<String, String> ecsMetrics = new HashMap<>();
    ecsMetrics.put("filter3", "ecs.fargate.cpu.user, ecs.fargate.mem.rss, ecs.fargate.mem.usage");

    DatadogCVServiceConfiguration cvServiceConfiguration = DatadogCVServiceConfiguration.builder()
                                                               .dockerMetrics(dockerMetrics)
                                                               .ecsMetrics(ecsMetrics)
                                                               .customMetrics(new HashMap<>())
                                                               .build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.DATA_DOG, cvServiceConfiguration);

    assertThat(actualDefinitions).hasSize(6);

    List<String> expectedKeySet = Lists.newArrayList("Docker CPU Usage", "Docker RSS Memory (%)",
        "Docker CPU Throttled", "ECS Container CPU Usage", "ECS Container RSS Memory", "ECS Container Memory Usage");
    Collections.sort(expectedKeySet);

    List<String> actualKeySet = new ArrayList<>(actualDefinitions.keySet());
    Collections.sort(actualKeySet);

    assertArrayEquals(expectedKeySet.toArray(), actualKeySet.toArray());

    expectedKeySet.forEach(key -> {
      TimeSeriesMetricDefinition definition = actualDefinitions.get(key);

      assertThat(definition.getMetricName()).isEqualTo(key);
      assertThat(definition.getMetricType()).isEqualTo(MetricType.INFRA);
    });
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricsCloudWatch() {
    List<CloudWatchMetric> cloudWatchMetrics = new ArrayList<>();

    cloudWatchMetrics.add(CloudWatchMetric.builder().metricName("Latency").metricType(MetricType.ERROR.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("RequestCount").metricType(MetricType.THROUGHPUT.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("EstimatedProcessedBytes").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("CPUReservation").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("CPUUtilization").metricType(MetricType.VALUE.name()).build());

    cloudWatchMetrics.add(
        CloudWatchMetric.builder().metricName("DiskReadBytes").metricType(MetricType.VALUE.name()).build());

    Map<String, List<CloudWatchMetric>> loadBalancerMetric = new HashMap<>();
    loadBalancerMetric.put("filter1", cloudWatchMetrics.subList(0, 2));
    loadBalancerMetric.put("filter2", cloudWatchMetrics.subList(2, 3));

    Map<String, List<CloudWatchMetric>> ecsMetrics = new HashMap<>();
    ecsMetrics.put("filter3", cloudWatchMetrics.subList(3, 4));

    List<CloudWatchMetric> ec2Metrics = cloudWatchMetrics.subList(4, 6);

    CloudWatchCVServiceConfiguration cvServiceConfiguration = CloudWatchCVServiceConfiguration.builder()
                                                                  .loadBalancerMetrics(loadBalancerMetric)
                                                                  .ecsMetrics(ecsMetrics)
                                                                  .ec2Metrics(ec2Metrics)
                                                                  .build();

    Map<String, TimeSeriesMetricDefinition> actualDefinitions =
        cvConfigurationService.getMetricDefinitionMap(StateType.CLOUD_WATCH, cvServiceConfiguration);

    assertThat(actualDefinitions).hasSize(6);

    cloudWatchMetrics.forEach(metric -> {
      TimeSeriesMetricDefinition definition = actualDefinitions.get(metric.getMetricName());
      assertThat(definition.getMetricName()).isEqualTo(metric.getMetricName());
      assertThat(definition.getMetricType().name()).isEqualTo(metric.getMetricType());
    });
  }
}