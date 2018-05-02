package software.wings.sm.states;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;
import software.wings.WingsBaseTest;
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
    List<List<APMMetricInfo>> metricEndpointsInfo = DatadogState.metricEndpointsInfo(
        "todolist", Lists.newArrayList("trace.servlet.request.duration", "system.cpu.iowait"));
    assertEquals(1, metricEndpointsInfo.size());
    List<APMMetricInfo> apmMetricInfos = metricEndpointsInfo.get(0);
    assertEquals("[{\"url\":\"query\",\"metricName\":\"Request Duration\",\"headers\":{},\"options\":"
            + "{\"from\":\"${start_time}\",\"to\":\"${end_time}\",\"api_key\":\"${apiKey}\","
            + "\"application_key\":\"${applicationKey}\",\"query\":\"trace.servlet.request.duration{"
            + "service:todolist, host:${host}}by{resource_name, host}.rollup(avg,60)\"},\""
            + "responseMappers\":[{\"fieldName\":\"txnName\",\"jsonPath\":\"series[*].scope\""
            + ",\"regexs\":[\"(resource_name:(.*),)\",\"(?<=:).*(?=,)\"]},{\"fieldName\":\""
            + "host\",\"jsonPath\":\"series[*].scope\",\"regexs\":[\"((?<=host:)(.*))(?=,"
            + "resource_name)\"]},{\"fieldName\":\"timestamp\",\"jsonPath\":\"series[*]."
            + "pointlist[*].[0]\"},{\"fieldName\":\"value\",\"jsonPath\":\"series[*]."
            + "pointlist[*].[1]\"}],\"metricType\":{},\"tag\":\"Servlet\"},"
            + "{\"url\":\"query\",\"metricName\":\"IO Wait\",\"headers\":{},\""
            + "options\":{\"from\":\"${start_time}\",\"to\":\"${end_time}\",\""
            + "api_key\":\"${apiKey}\",\"application_key\":\"${applicationKey}\",\""
            + "query\":\"system.cpu.iowait{host:${host}}by{host}.rollup(avg,60)\"},\""
            + "responseMappers\":[{\"fieldName\":\"txnName\",\"jsonPath\":\""
            + "series[*].scope\"},{\"fieldName\":\"timestamp\",\"jsonPath\":\""
            + "series[*].pointlist[*].[0]\"},{\"fieldName\":\"value\",\""
            + "jsonPath\":\"series[*].pointlist[*].[1]\"},{\""
            + "fieldName\":\"host\",\"jsonPath\":\"series[*].scope\",\""
            + "regexs\":[\"((?<=host:)(.*))\"]}],\"tag\":\"System\"}]",
        JsonUtils.asJson(apmMetricInfos));
  }
}
