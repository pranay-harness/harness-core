package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatadogStateTest extends WingsBaseTest {
  @Test
  public void metrics() {
    Map<String, DatadogState.Metric> metrics =
        DatadogState.metrics(Lists.newArrayList("trace.servlet.request.duration"));
    assertTrue(metrics.containsKey("trace.servlet.request.duration"));
    assertEquals("Servlet", metrics.get("trace.servlet.request.duration").getDatadogMetricType());
    assertEquals(Sets.newHashSet("Servlet"), metrics.get("trace.servlet.request.duration").getTags());
    assertEquals("trace.servlet.request.duration", metrics.get("trace.servlet.request.duration").getMetricName());
    assertEquals("Request Duration", metrics.get("trace.servlet.request.duration").getDisplayName());
    assertEquals(MetricType.RESP_TIME, metrics.get("trace.servlet.request.duration").getMlMetricType());
  }

  @Test
  public void metricDefinitions() {
    Map<String, DatadogState.Metric> metrics =
        DatadogState.metrics(Lists.newArrayList("trace.servlet.request.duration", "system.cpu.iowait"));

    List<DatadogState.Metric> metricList = new ArrayList<>();
    metricList.add(metrics.get("trace.servlet.request.duration"));
    metricList.add(metrics.get("system.cpu.iowait"));
    Map<String, TimeSeriesMetricDefinition> metricDefinitionMap = DatadogState.metricDefinitions(metricList);

    assertTrue(metricDefinitionMap.containsKey("Request Duration"));
    assertTrue(metricDefinitionMap.containsKey("IO Wait"));
  }

  @Test
  public void metricEndpointsInfo() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(
        "todolist", Lists.newArrayList("system.cpu.iowait", "trace.servlet.request.duration"));
    assertEquals(2, metricEndpointsInfo.size());
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
  public void metricEndpointsInfoTransformation() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo("todolist", Lists.newArrayList("kubernetes.cpu.usage.total"));
    assertEquals(1, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.values().iterator().next();
    String query =
        "query?api_key=${apiKey}&application_key=${applicationKey}&from=${start_time_seconds}&to=${end_time_seconds}&query=kubernetes.cpu.usage.total{$harness_batch{pod_name:${host}, '|'}}by{pod_name}.rollup(avg,60)/1000000000/kubernetes.cpu.capacity{*}.rollup(avg,60)*100";
    assertEquals("Transformed query must be same", query, metricEndpointsInfo.keySet().iterator().next());
    assertEquals(
        "[{\"metricName\":\"K8 CPU Usage\",\"responseMappers\":{\"host\":{\"fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=pod_name:)([^,]*))\"]},\"value\":{\"fieldName\":\"value\",\"jsonPath\":\"series[*].pointlist[*].[1]\"},\"txnName\":{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].metric\",\"regexs\":[\"((?<=[(]|^)[^(]([^ /|+|-|*]*))\"]},\"timestamp\":{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*].pointlist[*].[0]\"}},\"metricType\":{},\"tag\":\"Kubernetes\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }

  @Test(expected = WingsException.class)
  public void testBadMetric() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo("todolist", Lists.newArrayList("dummyMetricName"));
  }

  @Test(expected = WingsException.class)
  public void testBadServiceName() {
    Map<String, List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(
        null, Lists.newArrayList("trace.servlet.request.duration", "system.cpu.iowait"));
  }
}
