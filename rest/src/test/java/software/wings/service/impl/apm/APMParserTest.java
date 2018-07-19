package software.wings.service.impl.apm;

import static org.junit.Assert.assertEquals;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.serializer.YamlUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.states.APMVerificationState;
import software.wings.sm.states.DatadogState;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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
    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(textLoad)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .metricInfos(metricInfoIterator.next())
                                                         .build(),
            APMResponseParser.APMResponseData.builder()
                .text(textMem)
                .groupName(DEFAULT_GROUP_NAME)
                .metricInfos(metricInfoIterator.next())
                .build()));

    assertEquals(80, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/datadog_sample_collected_response.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void testJsonParserGraphana() throws IOException {
    String text500 =
        Resources.toString(APMParserTest.class.getResource("/apm/graphana_sample_response_500.json"), Charsets.UTF_8);

    Map<String, APMMetricInfo.ResponseMapper> responseMapperMap = new HashMap<>();
    responseMapperMap.put(
        "host", APMMetricInfo.ResponseMapper.builder().fieldName("host").jsonPath("[*].target").build());
    responseMapperMap.put("timestamp",
        APMMetricInfo.ResponseMapper.builder().fieldName("timestamp").jsonPath("[*].datapoints[*].[1]").build());
    responseMapperMap.put(
        "value", APMMetricInfo.ResponseMapper.builder().fieldName("value").jsonPath("[*].datapoints[*].[0]").build());
    responseMapperMap.put(
        "txnName", APMMetricInfo.ResponseMapper.builder().fieldName("txnName").fieldValue("500X").build());

    List<APMMetricInfo> metricInfos = Lists.newArrayList(APMMetricInfo.builder()
                                                             .metricName("500")
                                                             .metricType(MetricType.ERROR)
                                                             .tag("nginix")
                                                             .responseMappers(responseMapperMap)
                                                             .build());

    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(text500)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .metricInfos(metricInfos)
                                                         .build()));

    assertEquals(61, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/graphana_sample_collected_response_500.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void testJsonParserGraphanaMapping2() throws IOException {
    String text500 =
        Resources.toString(APMParserTest.class.getResource("/apm/graphana_sample_response_500.json"), Charsets.UTF_8);

    Map<String, APMMetricInfo.ResponseMapper> responseMapperMap = new HashMap<>();
    responseMapperMap.put(
        "host", APMMetricInfo.ResponseMapper.builder().fieldName("host").jsonPath("[0].target").build());
    responseMapperMap.put("timestamp",
        APMMetricInfo.ResponseMapper.builder().fieldName("timestamp").jsonPath("[0].datapoints[*].[1]").build());
    responseMapperMap.put(
        "value", APMMetricInfo.ResponseMapper.builder().fieldName("value").jsonPath("[0].datapoints[*].[0]").build());
    responseMapperMap.put(
        "txnName", APMMetricInfo.ResponseMapper.builder().fieldName("txnName").fieldValue("500X").build());

    List<APMMetricInfo> metricInfos = Lists.newArrayList(APMMetricInfo.builder()
                                                             .metricName("500")
                                                             .metricType(MetricType.ERROR)
                                                             .tag("nginix")
                                                             .responseMappers(responseMapperMap)
                                                             .build());

    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(text500)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .metricInfos(metricInfos)
                                                         .build()));

    assertEquals(61, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/graphana_sample_collected_response_500.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void testJsonParserGraphanaMappingArray() throws IOException {
    String text500 = Resources.toString(
        APMParserTest.class.getResource("/apm/graphana_sample_response_500_multiple.json"), Charsets.UTF_8);

    Map<String, APMMetricInfo.ResponseMapper> responseMapperMap = new HashMap<>();
    responseMapperMap.put(
        "host", APMMetricInfo.ResponseMapper.builder().fieldName("host").jsonPath("[*].target").build());
    responseMapperMap.put("timestamp",
        APMMetricInfo.ResponseMapper.builder().fieldName("timestamp").jsonPath("[*].datapoints[*].[1]").build());
    responseMapperMap.put(
        "value", APMMetricInfo.ResponseMapper.builder().fieldName("value").jsonPath("[*].datapoints[*].[0]").build());
    responseMapperMap.put(
        "txnName", APMMetricInfo.ResponseMapper.builder().fieldName("txnName").fieldValue("500X").build());

    List<APMMetricInfo> metricInfos = Lists.newArrayList(APMMetricInfo.builder()
                                                             .metricName("500")
                                                             .metricType(MetricType.ERROR)
                                                             .tag("nginix")
                                                             .responseMappers(responseMapperMap)
                                                             .build());

    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(text500)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .metricInfos(metricInfos)
                                                         .build()));

    assertEquals(4, records.size());
    String output = Resources.toString(
        APMParserTest.class.getResource("/apm/graphana_sample_collected_response_500_multiple.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void testJsonParserInsightsResponse() throws IOException {
    String text500 =
        Resources.toString(APMParserTest.class.getResource("/apm/insights_sample_response.json"), Charsets.UTF_8);

    Map<String, APMMetricInfo.ResponseMapper> responseMapperMap = new HashMap<>();
    responseMapperMap.put(
        "host", APMMetricInfo.ResponseMapper.builder().fieldName("host").jsonPath("facets[*].name[1]").build());
    responseMapperMap.put("timestamp",
        APMMetricInfo.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath("facets[*].timeSeries[*].endTimeSeconds")
            .build());
    responseMapperMap.put("value",
        APMMetricInfo.ResponseMapper.builder()
            .fieldName("value")
            .jsonPath("facets[*].timeSeries[*].results[*].count")
            .build());
    responseMapperMap.put(
        "txnName", APMMetricInfo.ResponseMapper.builder().fieldName("txnName").jsonPath("facets[*].name[0]").build());

    List<APMMetricInfo> metricInfos = Lists.newArrayList(APMMetricInfo.builder()
                                                             .metricName("HttpErrors")
                                                             .metricType(MetricType.ERROR)
                                                             .tag("NRHTTP")
                                                             .responseMappers(responseMapperMap)
                                                             .build());

    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(text500)
                                                         .metricInfos(metricInfos)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .build()));

    assertEquals(10, records.size());
    String output =
        Resources.toString(APMParserTest.class.getResource("/apm/insights_sample_collected.json"), Charsets.UTF_8);

    assertEquals(output, JsonUtils.asJson(records));
  }

  @Test
  public void testJsonParserArrayInsideArray() throws IOException {
    String text500 =
        Resources.toString(APMParserTest.class.getResource("/apm/insights-variation-1.json"), Charsets.UTF_8);

    Map<String, APMMetricInfo.ResponseMapper> responseMapperMap = new HashMap<>();
    responseMapperMap.put(
        "host", APMMetricInfo.ResponseMapper.builder().fieldName("host").jsonPath("facets[*].name[1]").build());
    responseMapperMap.put("timestamp",
        APMMetricInfo.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath("facets[*].timeSeries[*].endTimeSeconds")
            .build());
    responseMapperMap.put("value",
        APMMetricInfo.ResponseMapper.builder()
            .fieldName("value")
            .jsonPath("facets[*].timeSeries[*].results[*].[*].count")
            .build());
    responseMapperMap.put(
        "txnName", APMMetricInfo.ResponseMapper.builder().fieldName("txnName").jsonPath("facets[*].name[0]").build());

    List<APMMetricInfo> metricInfos = Lists.newArrayList(APMMetricInfo.builder()
                                                             .metricName("HttpErrors")
                                                             .metricType(MetricType.ERROR)
                                                             .tag("NRHTTP")
                                                             .responseMappers(responseMapperMap)
                                                             .build());

    Collection<NewRelicMetricDataRecord> records =
        APMResponseParser.extract(Lists.newArrayList(APMResponseParser.APMResponseData.builder()
                                                         .text(text500)
                                                         .metricInfos(metricInfos)
                                                         .groupName(DEFAULT_GROUP_NAME)
                                                         .build()));

    assertEquals(10, records.size());
    String output =
        Resources.toString(APMParserTest.class.getResource("/apm/insights-variation-1-collected.json"), Charsets.UTF_8);

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
