/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.logs;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.DatadogConfig;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.log.LogResponseParser;
import software.wings.service.impl.log.LogResponseParser.LogResponseData;
import software.wings.sm.states.BugsnagState;
import software.wings.sm.states.CustomLogVerificationState;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;
import software.wings.sm.states.DatadogLogState;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LogResponseParserTest extends CategoryTest {
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testParseValidResponse() throws IOException {
    String textLoad =
        Resources.toString(LogResponseParserTest.class.getResource("/apm/sampleElkResponse.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5909-192-7bd4987549-pfwgn");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("@timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("kubernetes.pod.name"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("log"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), false, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testParseValidResponseMultiple() throws IOException {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5922-198-b5bbc487d-bcqx6",
        "harness-manager-1.0.5919-197-bbd6df89f-mzrw2", "harness-manager-1.0.5922-198-b5bbc487d-rqx6p",
        "harness-manager-1.0.5922-199-656965f8ff-pcq4j", "harness-manager-1.0.5919-197-bbd6df89f-bg57m");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("hits.hits[*]._source.@timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("hits.hits[*]._source.kubernetes.pod.name"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("hits.hits[*]._source.log"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), true, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
    assertThat(5 == logs.size()).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testParseValidAzure() throws IOException {
    String textLoad =
        Resources.toString(LogResponseParserTest.class.getResource("/apm/azuresample.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5909-192-7bd4987549-pfwgn");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("tables[*].rows[*].[1]"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder().fieldName("host").fieldValue("samplehostname").build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("tables[*].rows[*].[0]"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), false, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testParseValidBugsnag() throws IOException {
    String textLoad =
        Resources.toString(LogResponseParserTest.class.getResource("/apm/bugsnagsample.json"), Charsets.UTF_8);

    Map<String, ResponseMapper> responseMappers =
        BugsnagState.constructLogDefinitions("sampleProjecId", null).values().iterator().next();
    List<String> hostList = Arrays.asList("harness-manager-1.0.5909-192-7bd4987549-pfwgn");
    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), false, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testParseValidResponse_doHostBasedFiltering() throws IOException {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);

    List<String> hostList = Arrays.asList("harness-manager-1.0.5922-198-b5bbc487d-bcqx6");
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("hits.hits[*]._source.@timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("hits.hits[*]._source.kubernetes.pod.name"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("hits.hits[*]._source.log"))
            .build());

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), true, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
    assertThat(1 == logs.size()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testParseValidResponse_datadogHostFromTag() throws IOException {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/datadog_log_json_response.json"), Charsets.UTF_8);
    DatadogConfig datadogConfig = DatadogConfig.builder().url("https://app․datadoghq․com/").build();

    List<String> hostList = Collections.singletonList("harness-example-deployment-canary-68659cc85f-szjs7");
    Map<String, Map<String, ResponseMapper>> response =
        DatadogLogState.constructLogDefinitions(datadogConfig, "pod_name", false);
    Map<String, ResponseMapper> responseMappers = response.get("https://app․datadoghq․com/logs-queries/list");

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), true, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
    assertThat(logs.size()).isEqualTo(2);
    for (LogElement logElement : logs) {
      assertThat(logElement.getHost()).isEqualTo(hostList.get(0));
      assertThat(logElement.getLogMessage()).isNotNull();
      assertThat(logElement.getTimeStamp()).isNotNull();
    }
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testParseValidResponse_datadogReservedHostField() throws IOException {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/datadog_log_json_response.json"), Charsets.UTF_8);
    DatadogConfig datadogConfig = DatadogConfig.builder().url("https://app․datadoghq․com/").build();

    List<String> hostList =
        Collections.singletonList("gke-cv-test-instana-pool-1-b8420bad-gqc2.c.playground-243019.internal");
    Map<String, Map<String, ResponseMapper>> response =
        DatadogLogState.constructLogDefinitions(datadogConfig, "host", false);
    Map<String, ResponseMapper> responseMappers = response.get("https://app․datadoghq․com/logs-queries/list");

    LogResponseParser parser = new LogResponseParser();
    LogResponseParser.LogResponseData data =
        new LogResponseData(textLoad, new HashSet<>(hostList), true, false, responseMappers);

    List<LogElement> logs = parser.extractLogs(data);
    assertThat(logs).isNotNull();
    assertThat(logs.size()).isEqualTo(2);
    for (LogElement logElement : logs) {
      assertThat(logElement.getHost()).isEqualTo(hostList.get(0));
      assertThat(logElement.getLogMessage()).isNotNull();
      assertThat(logElement.getTimeStamp()).isNotNull();
    }
  }
}
