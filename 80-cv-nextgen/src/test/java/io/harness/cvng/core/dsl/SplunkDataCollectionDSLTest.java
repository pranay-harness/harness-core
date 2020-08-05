package io.harness.cvng.core.dsl;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.rule.Owner;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplunkDataCollectionDSLTest extends CategoryTest {
  private DataCollectionDSLService dataCollectionDSLService;
  private String code;

  @ClassRule
  public static final HoverflyRule rule = HoverflyRule.inCaptureOrSimulationMode(
      Paths.get(SplunkDataCollectionDSLTest.class.getPackage().getName().replace(".", "/"), "splunk-response.json")
          .toString(),
      HoverflyConfig.localConfigs().disableTlsVerification());

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_splunkDSL() throws IOException {
    dataCollectionDSLService = new DataCollectionServiceImpl();
    code = readDSL();
    Instant instant = Instant.parse("2020-08-03T07:16:16.719Z");
    Map<String, Object> params = new HashMap<>();
    params.put("query", "*");
    params.put("serviceInstanceIdentifier", "$.host");
    params.put("maxCount", 10000);
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "**"); // Replace this with the actual value when capturing the request.

    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(instant.minusSeconds(15))
                                              .endTime(instant)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://splunk.dev.harness.io:8089")
                                              .build();
    List<LogDataRecord> logDataRecords =
        (List<LogDataRecord>) dataCollectionDSLService.execute(code, runtimeParameters);
    Assertions.assertThat(logDataRecords).isNotNull();
    assertThat(logDataRecords).hasSize(8);
    assertThat(logDataRecords.get(0).getHostname()).isEqualTo("harness-test-appd-deployment-6b494c889-m6v9w");
    assertThat(logDataRecords.get(0).getLog()).isEqualTo("java.lang.IllegalArgumentException: Please refer to the documentation.  at io.harness.ArgumentCheker.verifyArgument(ArgumentCheker.java:13)  at inside.RequestException.doGet(RequestException.java:104)  at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)  at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)  at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)  at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)  at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)  at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)  at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)  at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:199)  at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)  at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:493)  at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:137)  at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)  at org.apache.catalina.valves.AbstractAccessLogValve.invoke(AbstractAccessLogValve.java:660)  at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)  at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343)  at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:798)  at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)  at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:808)  at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1498)  at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)  at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)  at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)  at java.lang.Thread.run(Thread.java:748) ");
    assertThat(logDataRecords.get(0).getTimestamp()).isEqualTo(1596438974000L);
  }

  private String readDSL() throws IOException {
    return Resources.toString(SplunkCVConfig.class.getResource("splunk.datacollection"), StandardCharsets.UTF_8);
  }
}