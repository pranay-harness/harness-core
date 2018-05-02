package software.wings.service.impl.apm;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.states.DatadogState;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class APMParserTest extends WingsBaseTest {
  @Test
  public void testJsonParser() throws IOException {
    String textLoad =
        Resources.toString(APMParserTest.class.getResource("/apm/datadog_sample_response_load.json"), Charsets.UTF_8);
    String textMem =
        Resources.toString(APMParserTest.class.getResource("/apm/datadog_sample_response_mem.json"), Charsets.UTF_8);

    List<List<APMMetricInfo>> metricEndpointsInfo =
        DatadogState.metricEndpointsInfo("todolist", Lists.newArrayList("system.load.1", "system.mem.used"));
    Collection<NewRelicMetricDataRecord> records = APMResponseParser.extract(
        Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                               .metricName("system.load.1")
                               .text(textLoad)
                               .apmResponseMapper(metricEndpointsInfo.get(0).get(0).getResponseMappers())
                               .build(),
            APMResponseParser.APMResponseData.builder()
                .metricName("system.mem.used")
                .text(textMem)
                .apmResponseMapper(metricEndpointsInfo.get(0).get(1).getResponseMappers())
                .build()));

    assertEquals(40, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/datadog_sample_collected_response.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }
}
