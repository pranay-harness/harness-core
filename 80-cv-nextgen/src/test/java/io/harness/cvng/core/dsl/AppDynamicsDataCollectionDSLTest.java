package io.harness.cvng.core.dsl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.rule.Owner;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.SimulationSource;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.Before;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppDynamicsDataCollectionDSLTest extends CvNextGenTest {
  @Inject private MetricPackService metricPackService;
  private String accountId;
  private ExecutorService executorService;
  @ClassRule
  public static final HoverflyRule rule =
      HoverflyRule.inSimulationMode(HoverflyConfig.localConfigs().disableTlsVerification());
  /* public static final HoverflyRule rule =
    HoverflyRule.inCaptureMode(HoverflyConfig.localConfigs().disableTlsVerification()); */

  @Before
  public void setup() {
    accountId = generateUuid();
    executorService = Executors.newFixedThreadPool(10);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_appDyanmicsResourcePackForServiceGuard() throws IOException {
    String filePath = "appdynamics/performance-service-guard.json";
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/" + filePath)));
    // rule.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("performance-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1598017842368L);
    List<MetricPack> metricPacks = metricPackService.getMetricPacks(accountId, "project", DataSourceType.APP_DYNAMICS);

    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName("cv-app")
            .tierName("docker-tier")
            .metricPack(metricPacks.stream()
                            .filter(metricPack -> metricPack.getIdentifier().equals("Performance"))
                            .findFirst()
                            .get()
                            .toDTO())
            .build();
    Map<String, Object> params = appDynamicsDataCollectionInfo.getDslEnvVariables();

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic **"); // Replace this with the actual value when capturing the request.
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(instant.minusSeconds(60))
                                              .endTime(instant)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://harness-test.saas.appdynamics.com/controller/")
                                              .build();
    List<TimeSeriesRecord> timeSeriesRecords =
        (List<TimeSeriesRecord>) dataCollectionDSLService.execute(code, runtimeParameters);
    assertThat(Sets.newHashSet(timeSeriesRecords))
        .isEqualTo(new Gson().fromJson(readJson("performance-service-guard-expectation.json"),
            new TypeToken<Set<TimeSeriesRecord>>() {}.getType()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_appDyanmicsPerformancePackWithHosts() throws IOException {
    String filePath = "appdynamics/performance-verification-task-collect-hosts.json";
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/" + filePath)));
    // rule.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("performance-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1598017842368L);
    List<MetricPack> metricPacks = metricPackService.getMetricPacks(accountId, "project", DataSourceType.APP_DYNAMICS);

    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName("cv-app")
            .tierName("docker-tier")
            .metricPack(metricPacks.stream()
                            .filter(metricPack -> metricPack.getIdentifier().equals("Performance"))
                            .findFirst()
                            .get()
                            .toDTO())
            .build();
    appDynamicsDataCollectionInfo.setCollectHostData(true);
    Map<String, Object> params = appDynamicsDataCollectionInfo.getDslEnvVariables();

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic **"); // Replace this with the actual value when capturing the request.
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(instant.minusSeconds(60))
                                              .endTime(instant)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://harness-test.saas.appdynamics.com/controller/")
                                              .build();
    List<TimeSeriesRecord> timeSeriesRecords =
        (List<TimeSeriesRecord>) dataCollectionDSLService.execute(code, runtimeParameters);
    assertThat(Sets.newHashSet(timeSeriesRecords))
        .isEqualTo(new Gson().fromJson(readJson("performance-collection-hosts-expectation.json"),
            new TypeToken<Set<TimeSeriesRecord>>() {}.getType()));
  }

  private String readDSL(String name) throws IOException {
    return Resources.toString(
        AppDynamicsCVConfig.class.getResource("/appdynamics/dsl/" + name), StandardCharsets.UTF_8);
  }

  private String readJson(String name) throws IOException {
    return Resources.toString(
        AppDynamicsDataCollectionDSLTest.class.getResource("/hoverfly/appdynamics/" + name), StandardCharsets.UTF_8);
  }
}
