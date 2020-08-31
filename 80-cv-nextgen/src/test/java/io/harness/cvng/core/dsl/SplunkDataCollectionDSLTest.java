package io.harness.cvng.core.dsl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.SplunkDataCollectionInfo;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.SimulationSource;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SplunkDataCollectionDSLTest extends CategoryTest {
  private DataCollectionDSLService dataCollectionDSLService;
  private String code;

  @ClassRule
  public static final HoverflyRule rule =
      HoverflyRule.inSimulationMode(HoverflyConfig.localConfigs().disableTlsVerification());
  /*@ClassRule
  public static final HoverflyRule rule =
      HoverflyRule.inCaptureMode(HoverflyConfig.localConfigs().disableTlsVerification());*/

  @Before
  public void setup() {
    dataCollectionDSLService = new DataCollectionServiceImpl();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_splunkDSL() throws IOException {
    String filePath = "splunk/splunk-response.json";
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/" + filePath)));
    // rule.capture(filePath);
    code = readDSL("splunk.datacollection");

    final RuntimeParameters runtimeParameters = getRuntimeParameters();
    List<LogDataRecord> logDataRecords =
        (List<LogDataRecord>) dataCollectionDSLService.execute(code, runtimeParameters);
    Assertions.assertThat(logDataRecords).isNotNull();
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
  public void testExecute_splunkHostDSL() throws IOException {
    String filePath = "splunk/splunk-response-host.json";
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/" + filePath)));
    // rule.capture(filePath);
    code = readDSL("splunk_host_collection.datacollection");
    final RuntimeParameters runtimeParameters = getRuntimeParameters();
    Set<String> hosts = new HashSet<>((Collection<String>) dataCollectionDSLService.execute(code, runtimeParameters));
    assertThat(hosts).hasSize(2);
    assertThat(hosts).isEqualTo(Sets.newHashSet(
        "harness-test-appd-deployment-5bd684f655-cslds", "harness-test-appd-deployment-5bd684f655-lqfrp"));
  }

  private String readDSL(String fileName) throws IOException {
    return Resources.toString(SplunkCVConfig.class.getResource(fileName), StandardCharsets.UTF_8);
  }

  private RuntimeParameters getRuntimeParameters() {
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
    Instant instant = Instant.parse("2020-08-28T11:06:44.711Z");
    return RuntimeParameters.builder()
        .baseUrl(dataCollectionInfo.getBaseUrl(splunkConnectorDTO))
        .commonHeaders(dataCollectionInfo.collectionHeaders(splunkConnectorDTO))
        .commonOptions(dataCollectionInfo.collectionParams(splunkConnectorDTO))
        .otherEnvVariables(dataCollectionInfo.getDslEnvVariables())
        .endTime(instant)
        .startTime(instant.minus(Duration.ofMinutes(1)))
        .build();
  }
}