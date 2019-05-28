package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.api.DeploymentType.KUBERNETES;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.apm.APMMetricInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DatadogStateTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void metrics() {
    Map<String, DatadogState.Metric> metrics =
        DatadogState.metrics(Optional.of(Lists.newArrayList("trace.servlet.request.duration")), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty());
    assertTrue(metrics.containsKey("trace.servlet.request.duration"));
    assertEquals("Servlet", metrics.get("trace.servlet.request.duration").getDatadogMetricType());
    assertEquals(Sets.newHashSet("Servlet"), metrics.get("trace.servlet.request.duration").getTags());
    assertEquals("trace.servlet.request.duration", metrics.get("trace.servlet.request.duration").getMetricName());
    assertEquals("Request Duration", metrics.get("trace.servlet.request.duration").getDisplayName());
    assertEquals(MetricType.RESP_TIME.name(), metrics.get("trace.servlet.request.duration").getMlMetricType());
  }

  @Test
  @Category(UnitTests.class)
  public void metricDefinitions() {
    Map<String, DatadogState.Metric> metrics =
        DatadogState.metrics(Optional.of(Lists.newArrayList("trace.servlet.request.duration", "system.cpu.iowait")),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    List<DatadogState.Metric> metricList = new ArrayList<>();
    metricList.add(metrics.get("trace.servlet.request.duration"));
    metricList.add(metrics.get("system.cpu.iowait"));
    Map<String, TimeSeriesMetricDefinition> metricDefinitionMap = DatadogState.metricDefinitions(metricList);

    assertTrue(metricDefinitionMap.containsKey("Request Duration"));
    assertTrue(metricDefinitionMap.containsKey("IO Wait"));
  }

  @Test
  @Category(UnitTests.class)
  public void metricEndpointsInfo() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(Optional.of("todolist"),
        Optional.of(Lists.newArrayList("system.cpu.iowait", "trace.servlet.request.duration")), Optional.empty(),
        Optional.empty(), Optional.of(KUBERNETES));
    assertEquals(4, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.values().iterator().next();
    for (String query : metricEndpointsInfo.keySet()) {
      if (query.contains("system.cpu.iowait")) {
        apmMetricInfos = metricEndpointsInfo.get(query);
      }
    }
    assertEquals(
        "[{\"metricName\":\"IO Wait\",\"responseMappers\":{\"host\":{\"fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=host:)([^,]*))\"]},\"value\":{\"fieldName\":\"value\",\"jsonPath\":\"series[*].pointlist[*].[1]\"},\"txnName\":{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].metric\"},\"timestamp\":{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*].pointlist[*].[0]\"}},\"metricType\":{},\"tag\":\"System\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }

  @Test
  @Category(UnitTests.class)
  public void metricEndpointsInfoTransformation() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(Optional.empty(),
        Optional.of(Lists.newArrayList("kubernetes.cpu.usage.total")), Optional.empty(), Optional.empty(),
        Optional.of(KUBERNETES));
    assertEquals(1, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.values().iterator().next();
    String query =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=kubernetes.cpu.usage.total{pod_name:${host}}by{pod_name}.rollup(avg,60)/1000000000/kubernetes.cpu.capacity{*}.rollup(avg,60)*100";
    assertEquals("Transformed query must be same", query, metricEndpointsInfo.keySet().iterator().next());
    assertEquals(
        "[{\"metricName\":\"K8 CPU Usage\",\"responseMappers\":{\"host\":{\"fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=pod_name:)([^,]*))\"]},\"value\":{\"fieldName\":\"value\",\"jsonPath\":\"series[*].pointlist[*].[1]\"},\"txnName\":{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].metric\",\"regexs\":[\"((?<=[(]|^)[^(]([^ /|+|-|*]*))\"]},\"timestamp\":{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*].pointlist[*].[0]\"}},\"metricType\":{},\"tag\":\"Kubernetes\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }

  @Test
  @Category(UnitTests.class)
  public void metricEndpointsInfoDocker() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo(Optional.empty(), Optional.of(Lists.newArrayList("docker.cpu.usage")),
            Optional.empty(), Optional.empty(), Optional.of(KUBERNETES));
    assertEquals(1, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.values().iterator().next();
    String query =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=docker.cpu.usage{pod_name:${host}}by{pod_name}.rollup(avg,60)";
    assertEquals("Transformed query must be same", query, metricEndpointsInfo.keySet().iterator().next());
    assertEquals(
        "[{\"metricName\":\"Docker CPU Usage\",\"responseMappers\":{\"host\":{\"fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=pod_name:)([^,]*))\"]},\"value\":{\"fieldName\":\"value\",\"jsonPath\":\"series[*].pointlist[*].[1]\"},\"txnName\":{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].metric\",\"regexs\":[\"((?<=[(]|^)[^(]([^ /|+|-|*]*))\"]},\"timestamp\":{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*].pointlist[*].[0]\"}},\"metricType\":{},\"tag\":\"Docker\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }

  @Test
  @Category(UnitTests.class)
  public void metricEndpointsInfoDocker24x7() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo(Optional.of("todolist"), Optional.of(Lists.newArrayList("docker.cpu.usage")),
            Optional.of("cluster:harness-test"), Optional.empty(), Optional.empty());
    assertEquals(4, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.values().iterator().next();
    String query =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=docker.cpu.usage{cluster:harness-test}.rollup(avg,60)";
    assertEquals("Transformed query must be same", query, metricEndpointsInfo.keySet().iterator().next());
    assertEquals(
        "[{\"metricName\":\"Docker CPU Usage\",\"responseMappers\":{\"host\":{\"fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=pod_name:)([^,]*))\"]},\"value\":{\"fieldName\":\"value\",\"jsonPath\":\"series[*].pointlist[*].[1]\"},\"txnName\":{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].metric\",\"regexs\":[\"((?<=[(]|^)[^(]([^ /|+|-|*]*))\"]},\"timestamp\":{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*].pointlist[*].[0]\"}},\"metricType\":{},\"tag\":\"Docker\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testBadMetric() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo(Optional.of("todolist"), Optional.of(Lists.newArrayList("dummyMetricName")),
            Optional.empty(), Optional.empty(), Optional.of(KUBERNETES));
  }

  @Test
  @Category(UnitTests.class)
  public void testServletSystemMetrics() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(Optional.empty(),
        Optional.of(Lists.newArrayList("trace.servlet.request.duration", "system.cpu.iowait")), Optional.empty(),
        Optional.empty(), Optional.of(KUBERNETES));
    assertTrue(metricEndpointsInfo.size() == 2);
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceLevelMetrics() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(
        Optional.of("todolist"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(KUBERNETES));
    assertEquals(3, metricEndpointsInfo.size());
    Set<String> traceMetrics = new HashSet<>(Arrays.asList("Request Duration", "Errors", "Hits"));
    metricEndpointsInfo.forEach((k, v) -> {
      assertTrue(v.size() == 1);
      assertTrue(traceMetrics.contains(v.get(0).getMetricName()));
    });
  }
}
