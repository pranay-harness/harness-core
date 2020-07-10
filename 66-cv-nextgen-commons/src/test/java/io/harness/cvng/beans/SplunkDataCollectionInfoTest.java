package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenCommonBaseTest;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplunkDataCollectionInfoTest extends CVNextGenCommonBaseTest {
  private DataCollectionDSLService dataCollectionDSLService;
  private String code;

  @Before
  public void setup() throws IOException {}

  /**
   * TODO:
   * We need to find a way to mock the network call and write proper test.
   * This code is meant for testing DSL at the development time.
   */
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("This is making network call and for testing DSL for splunk. ")
  public void testExecute_splunkDSL() throws IOException {
    dataCollectionDSLService = new DataCollectionServiceImpl();
    code = readDSL();
    Instant now = Instant.now();
    Map<String, Object> params = new HashMap<>();
    params.put("query", "*");
    params.put("serviceInstanceIdentifier", "$.host");
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "**");

    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(now.minusSeconds(300))
                                              .endTime(now)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://splunk.dev.harness.io:8089")
                                              .build();
    List<LogDataRecord> logDataRecords =
        (List<LogDataRecord>) dataCollectionDSLService.execute(code, runtimeParameters);
    assertThat(logDataRecords).isNotNull();
  }

  private String readDSL() throws IOException {
    return Resources.toString(SplunkDataCollectionInfoTest.class.getResource("splunk.dsl"), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDslEnvVariables() {
    SplunkDataCollectionInfo splunkDataCollectionInfo =
        SplunkDataCollectionInfo.builder().query("exception").serviceInstanceIdentifier("host").build();
    Map<String, Object> expected = new HashMap<>();
    expected.put("query", "exception");
    expected.put("serviceInstanceIdentifier", "$.host");
    expected.put("maxCount", 10000);
    assertThat(splunkDataCollectionInfo.getDslEnvVariables()).isEqualTo(expected);
  }
}