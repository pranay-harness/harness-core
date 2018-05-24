package software.wings.service.impl.apm;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.states.APMVerificationState;
import software.wings.sm.states.DatadogState;
import software.wings.utils.JsonUtils;
import software.wings.utils.YamlUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class APMParserTest extends WingsBaseTest {
  @Test
  public void testJsonParser() throws IOException {
    String textLoad =
        Resources.toString(APMParserTest.class.getResource("/apm/datadog_sample_response_load.json"), Charsets.UTF_8);
    String textMem =
        Resources.toString(APMParserTest.class.getResource("/apm/datadog_sample_response_mem.json"), Charsets.UTF_8);

    Map<String, List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo("todolist", Lists.newArrayList("system.load.1", "system.mem.used"));

    Iterator<List<APMMetricInfo>> metricInfoIterator = metricEndpointsInfo.values().iterator();
    Collection<NewRelicMetricDataRecord> records = APMResponseParser.extract(Lists.newArrayList(
        APMResponseParser.APMResponseData.builder().text(textLoad).metricInfos(metricInfoIterator.next()).build(),
        APMResponseParser.APMResponseData.builder().text(textMem).metricInfos(metricInfoIterator.next()).build()));

    assertEquals(80, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/datadog_sample_collected_response.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void apmVerificationstateYaml() throws IOException {
    String yaml =
        "- collectionUrl: query?from=${start_time}&to=${end_time}&query=trace.servlet.request.duration{service:todolist, host:${host}}by{resource_name, host}.rollup(avg,60)\n"
        + "  metricName: Request Duration\n"
        + "  metricType: RESP_TIME\n"
        + "  tag: Servlet\n"
        + "  method: POST\n"
        + "  responseMappings:\n"
        + "    txnNameJsonPath: series[*].scope\n"
        + "    txnNameRegex: ((?<=resource_name:)(.*)(?=,))  \n"
        + "    timestampJsonPath: series[*].pointlist[*].[0]\n"
        + "    metricValueJsonPath: series[*].pointlist[*].[1]";
    YamlUtils yamlUtils = new YamlUtils();
    yamlUtils.read(yaml, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
  }
}
