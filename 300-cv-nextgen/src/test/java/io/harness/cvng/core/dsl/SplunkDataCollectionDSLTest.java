/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.dsl;

import static io.harness.CvNextGenTestBase.getResourceFilePath;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.HoverflyTestBase;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.beans.splunk.SplunkSavedSearchRequest;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.beans.cvng.splunk.SplunkConnectorValidationInfo;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.specto.hoverfly.junit.core.SimulationSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SplunkDataCollectionDSLTest extends HoverflyTestBase {
  private DataCollectionDSLService dataCollectionDSLService;
  private String code;

  @Before
  public void setup() {
    dataCollectionDSLService = new DataCollectionServiceImpl();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkDSL() throws IOException {
    String filePath = "splunk/splunk-response.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    code = readDSL("splunk.datacollection");

    final RuntimeParameters runtimeParameters = getRuntimeParameters(Instant.parse("2020-08-28T11:06:44.711Z"));
    List<LogDataRecord> logDataRecords =
        (List<LogDataRecord>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(logDataRecords).isNotNull();
    assertThat(logDataRecords).hasSize(6);
    assertThat(logDataRecords.get(0).getHostname()).isEqualTo("harness-test-appd-deployment-5bd684f655-cslds");
    assertThat(logDataRecords.get(0).getLog())
        .isEqualTo(
            "java.lang.RuntimeException: javax.activity.InvalidActivityException: Invalid activity sleep  at invalid.InvalidExceptionGenerator.generateInvalidActivityException(InvalidExceptionGenerator.java:17)  at inside.RequestException.doGet(RequestException.java:131)  at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)  at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)  at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)  at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)  at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)  at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)  at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)  at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:199)  at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)  at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:493)  at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:137)  at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)  at org.apache.catalina.valves.AbstractAccessLogValve.invoke(AbstractAccessLogValve.java:660)  at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)  at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343)  at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:798)  at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)  at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:808)  at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1498)  at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)  at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)  at java.lang.Thread.run(Thread.java:748) Caused by: javax.activity.InvalidActivityException: Invalid activity sleep  ... 26 more ");
    assertThat(logDataRecords.get(0).getTimestamp()).isEqualTo(1598612779000L);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkHostDSL() throws IOException {
    String filePath = "splunk/splunk-response-host.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    code = readDSL("splunk_host_collection.datacollection");
    final RuntimeParameters runtimeParameters = getRuntimeParameters(Instant.parse("2020-11-18T08:52:57.079Z"));
    Set<String> hosts = new HashSet<>(
        (Collection<String>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {}));
    assertThat(hosts).hasSize(3);
    assertThat(hosts).isEqualTo(Sets.newHashSet("harness-test-appd-deployment-canary-5bb85ff9f4-9lpl9",
        "harness-test-appd-deployment-77b974d77-m4w7x", "harness-test-appd-deployment-77b974d77-f7hlb"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkHostDSLEmptyResponse() throws IOException {
    String filePath = "splunk/splunk-response-host_empty.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    code = readDSL("splunk_host_collection.datacollection");
    final RuntimeParameters runtimeParameters = getRuntimeParameters(Instant.parse("2020-08-28T11:06:44.711Z"));
    Set<String> hosts = new HashSet<>(
        (Collection<String>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {}));
    assertThat(hosts).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkConnectionValidationValidSettings() {
    String filePath = "splunk/connection-validation-valid.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    SplunkConnectorValidationInfo splunkConnectorValidationInfo = SplunkConnectorValidationInfo.builder().build();
    splunkConnectorValidationInfo.setConnectorConfigDTO(
        SplunkConnectorDTO.builder()
            .splunkUrl("https://splunk.dev.harness.io:8089/")
            .username("harnessadmin")
            .passwordRef(SecretRefData.builder().decryptedValue("Harness@123".toCharArray()).build())
            .build());
    String code = splunkConnectorValidationInfo.getConnectionValidationDSL();
    Instant instant = Instant.parse("2020-10-30T10:44:48.164Z");
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(splunkConnectorValidationInfo.getStartTime(instant))
                                              .endTime(splunkConnectorValidationInfo.getEndTime(instant))
                                              .commonHeaders(splunkConnectorValidationInfo.collectionHeaders())
                                              .baseUrl(splunkConnectorValidationInfo.getBaseUrl())
                                              .build();
    String isValid = (String) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(isValid).isEqualTo("true");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkConnectionValidationInValidSettings() {
    String filePath = "splunk/connection-validation-invalid.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    SplunkConnectorValidationInfo splunkConnectorValidationInfo = SplunkConnectorValidationInfo.builder().build();
    splunkConnectorValidationInfo.setConnectorConfigDTO(
        SplunkConnectorDTO.builder()
            .splunkUrl("https://splunk.dev.harness.io:8089/invalid")
            .username("harnessadmin")
            .passwordRef(SecretRefData.builder().decryptedValue("Harness@123".toCharArray()).build())
            .build());
    String code = splunkConnectorValidationInfo.getConnectionValidationDSL();
    Instant instant = Instant.parse("2020-10-30T10:44:48.164Z");
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(splunkConnectorValidationInfo.getStartTime(instant))
                                              .endTime(splunkConnectorValidationInfo.getEndTime(instant))
                                              .commonHeaders(splunkConnectorValidationInfo.collectionHeaders())
                                              .baseUrl(splunkConnectorValidationInfo.getBaseUrl())
                                              .build();
    assertThatThrownBy(() -> dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {}))
        .hasMessage(
            "io.harness.datacollection.exception.DataCollectionException: io.harness.datacollection.exception.DataCollectionException: Response code: 405 Error: Response{protocol=http/1.1, code=405, message=Method Not Allowed, url=https://splunk.dev.harness.io:8089/invalid/services/search/jobs/?output_mode=json&exec_mode=blocking}");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_splunkSavedSearches() throws IOException {
    String filePath = "splunk/saved-searches.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    SplunkSavedSearchRequest splunkSavedSearchRequest =
        SplunkSavedSearchRequest.builder()
            .connectorInfoDTO(
                ConnectorInfoDTO.builder()
                    .connectorConfig(
                        SplunkConnectorDTO.builder()
                            .splunkUrl("https://splunk.dev.harness.io:8089/")
                            .username("harnessadmin")
                            .passwordRef(SecretRefData.builder().decryptedValue("Harness@123".toCharArray()).build())
                            .build())
                    .build())
            .build();

    Instant instant = Instant.parse("2020-10-30T10:44:48.164Z");
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(splunkSavedSearchRequest.getStartTime(instant))
                                              .endTime(splunkSavedSearchRequest.getEndTime(instant))
                                              .commonHeaders(splunkSavedSearchRequest.collectionHeaders())
                                              .baseUrl(splunkSavedSearchRequest.getBaseUrl())
                                              .build();
    List<?> result = (List<?>) dataCollectionDSLService.execute(
        splunkSavedSearchRequest.getDSL(), runtimeParameters, callDetails -> {});
    assertThat(result).hasSize(8);
    assertThat(result).isEqualTo(
        new Gson().fromJson(readJson("saved-searches-expectation.json"), new TypeToken<List<Map>>() {}.getType()));
  }

  private String readDSL(String fileName) throws IOException {
    return Resources.toString(SplunkCVConfig.class.getResource(fileName), StandardCharsets.UTF_8);
  }

  private RuntimeParameters getRuntimeParameters(Instant instant) {
    SplunkDataCollectionInfo dataCollectionInfo =
        SplunkDataCollectionInfo.builder().query("*").serviceInstanceIdentifier("host").build();
    dataCollectionInfo.setHostCollectionDSL(code);
    dataCollectionInfo.setCollectHostData(true);
    SplunkConnectorDTO splunkConnectorDTO =
        SplunkConnectorDTO.builder()
            .splunkUrl("https://splunk.dev.harness.io:8089/")
            .accountId(generateUuid())
            .username("harnessadmin")
            .passwordRef(SecretRefData.builder().decryptedValue("Harness@123".toCharArray()).build())
            .build();
    return RuntimeParameters.builder()
        .baseUrl(dataCollectionInfo.getBaseUrl(splunkConnectorDTO))
        .commonHeaders(dataCollectionInfo.collectionHeaders(splunkConnectorDTO))
        .commonOptions(dataCollectionInfo.collectionParams(splunkConnectorDTO))
        .otherEnvVariables(dataCollectionInfo.getDslEnvVariables(SplunkConnectorDTO.builder().build()))
        .endTime(instant)
        .startTime(instant.minus(Duration.ofMinutes(1)))
        .build();
  }

  private String readJson(String name) throws IOException {
    return FileUtils.readFileToString(
        new File(getResourceFilePath("/hoverfly/splunk/" + name)), StandardCharsets.UTF_8);
  }
}
