/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.dsl;

import static io.harness.CvNextGenTestBase.getResourceFilePath;
import static io.harness.CvNextGenTestBase.getSourceResourceFile;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cvng.HoverflyCVNextGenTestBase;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.datacollection.impl.DataCollectionServiceImpl;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.cvng.appd.AppDynamicsConnectorValidationInfo;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import io.specto.hoverfly.junit.core.SimulationSource;
import java.io.File;
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
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDataCollectionDSLTest extends HoverflyCVNextGenTestBase {
  @Inject private MetricPackService metricPackService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private ExecutorService executorService;

  @Before
  public void setup() {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    executorService = Executors.newFixedThreadPool(10);
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDyanmicsPerformancePackForServiceGuard() throws IOException {
    String filePath = "appdynamics/performance-service-guard.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("performance-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1599634954000L);
    List<MetricPack> metricPacks =
        metricPackService.getMetricPacks(accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS);

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
    Map<String, Object> params =
        appDynamicsDataCollectionInfo.getDslEnvVariables(AppDynamicsConnectorDTO.builder().build());

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
        (List<TimeSeriesRecord>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(Sets.newHashSet(timeSeriesRecords))
        .isEqualTo(new Gson().fromJson(readJson("performance-service-guard-expectation.json"),
            new TypeToken<Set<TimeSeriesRecord>>() {}.getType()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDyanmicsPerformancePackWithHosts() throws IOException {
    String filePath = "appdynamics/performance-verification-task-collect-hosts.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("performance-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1599634954000L);
    List<MetricPack> metricPacks =
        metricPackService.getMetricPacks(accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS);

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
    Map<String, Object> params =
        appDynamicsDataCollectionInfo.getDslEnvVariables(AppDynamicsConnectorDTO.builder().build());

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

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDyanmicsQualityPackForServiceGuard() throws IOException {
    String filePath = "appdynamics/quality-service-guard.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    //    		 HOVERFLY_RULE.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("quality-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1607498018484L);
    List<MetricPack> metricPacks =
        metricPackService.getMetricPacks(accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS);

    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName("cv-app")
            .tierName("docker-tier")
            .metricPack(
                metricPacks.stream()
                    .filter(
                        metricPack -> metricPack.getIdentifier().equals(CVMonitoringCategory.ERRORS.getDisplayName()))
                    .findFirst()
                    .get()
                    .toDTO())
            .build();
    Map<String, Object> params =
        appDynamicsDataCollectionInfo.getDslEnvVariables(AppDynamicsConnectorDTO.builder().build());

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic **"); // Replace this with the actual value when capturing the request.
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(instant.minusSeconds(600))
                                              .endTime(instant)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://harness-test.saas.appdynamics.com/controller/")
                                              .build();
    List<TimeSeriesRecord> timeSeriesRecords =
        (List<TimeSeriesRecord>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(Sets.newHashSet(timeSeriesRecords))
        .isEqualTo(new Gson().fromJson(
            readJson("quality-service-guard-expectation.json"), new TypeToken<Set<TimeSeriesRecord>>() {}.getType()));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDyanmicsQualityPackWithHosts() throws IOException {
    String filePath = "appdynamics/quality-verification-task-collect-hosts.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    //		HOVERFLY_RULE.capture(filePath);

    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    String code = readDSL("quality-pack.datacollection");
    Instant instant = Instant.ofEpochMilli(1607498018484L);
    List<MetricPack> metricPacks =
        metricPackService.getMetricPacks(accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS);

    AppDynamicsDataCollectionInfo appDynamicsDataCollectionInfo =
        AppDynamicsDataCollectionInfo.builder()
            .applicationName("cv-app")
            .tierName("docker-tier")
            .metricPack(
                metricPacks.stream()
                    .filter(
                        metricPack -> metricPack.getIdentifier().equals(CVMonitoringCategory.ERRORS.getDisplayName()))
                    .findFirst()
                    .get()
                    .toDTO())
            .build();
    appDynamicsDataCollectionInfo.setCollectHostData(true);
    Map<String, Object> params =
        appDynamicsDataCollectionInfo.getDslEnvVariables(AppDynamicsConnectorDTO.builder().build());

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic **"); // Replace this with the actual value when capturing the request.
    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(instant.minusSeconds(600))
                                              .endTime(instant)
                                              .commonHeaders(headers)
                                              .otherEnvVariables(params)
                                              .baseUrl("https://harness-test.saas.appdynamics.com/controller/")
                                              .build();
    List<TimeSeriesRecord> timeSeriesRecords =
        (List<TimeSeriesRecord>) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(Sets.newHashSet(timeSeriesRecords))
        .isEqualTo(new Gson().fromJson(readJson("quality-collection-hosts-expectation.json"),
            new TypeToken<Set<TimeSeriesRecord>>() {}.getType()));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDynamicsConnectionValidationValidSettings() {
    String filePath = "appdynamics/connection-validation-valid.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    AppDynamicsConnectorValidationInfo appDynamicsConnectorValidationInfo =
        AppDynamicsConnectorValidationInfo.builder().build();
    appDynamicsConnectorValidationInfo.setConnectorConfigDTO(
        AppDynamicsConnectorDTO.builder()
            .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
            .accountname("harness-test")
            .username("uitest@harness.io")
            .passwordRef(SecretRefData.builder().decryptedValue("**".toCharArray()).build())
            .build());
    String code = appDynamicsConnectorValidationInfo.getConnectionValidationDSL();
    Instant instant = Instant.now();
    System.out.println(instant);

    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(appDynamicsConnectorValidationInfo.getStartTime(instant))
                                              .endTime(appDynamicsConnectorValidationInfo.getEndTime(instant))
                                              .commonHeaders(appDynamicsConnectorValidationInfo.collectionHeaders())
                                              .baseUrl(appDynamicsConnectorValidationInfo.getBaseUrl())
                                              .build();
    String isValid = (String) dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {});
    assertThat(isValid).isEqualTo("true");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("https://harness.atlassian.net/browse/CVNG-1599")
  public void testExecute_appDynamicsConnectionValidationInValidSettings() {
    String filePath = "appdynamics/connection-validation-invalid.json";
    HOVERFLY_RULE.simulate(SimulationSource.file(Paths.get(getResourceFilePath("hoverfly/" + filePath))));
    // HOVERFLY_RULE.capture(filePath);
    DataCollectionDSLService dataCollectionDSLService = new DataCollectionServiceImpl();
    dataCollectionDSLService.registerDatacollectionExecutorService(executorService);
    AppDynamicsConnectorValidationInfo appDynamicsConnectorValidationInfo =
        AppDynamicsConnectorValidationInfo.builder().build();
    appDynamicsConnectorValidationInfo.setConnectorConfigDTO(
        AppDynamicsConnectorDTO.builder()
            .controllerUrl("https://harness-test.saas.appdynamics.com/controllerr")
            .accountname("harness-test")
            .username("uitest@harness.io")
            .passwordRef(SecretRefData.builder().decryptedValue("**".toCharArray()).build())
            .build());
    String code = appDynamicsConnectorValidationInfo.getConnectionValidationDSL();
    Instant instant = Instant.now();
    System.out.println(instant);

    RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                              .startTime(appDynamicsConnectorValidationInfo.getStartTime(instant))
                                              .endTime(appDynamicsConnectorValidationInfo.getEndTime(instant))
                                              .commonHeaders(appDynamicsConnectorValidationInfo.collectionHeaders())
                                              .baseUrl(appDynamicsConnectorValidationInfo.getBaseUrl())
                                              .build();
    assertThatThrownBy(() -> dataCollectionDSLService.execute(code, runtimeParameters, callDetails -> {}))
        .hasMessage(
            "io.harness.datacollection.exception.DataCollectionException: io.harness.datacollection.exception.DataCollectionException: Response code: 404 Error: Response{protocol=http/1.1, code=404, message=Not Found, url=https://harness-test.saas.appdynamics.com/controllerr/rest/applications?output=json}");
  }

  private String readDSL(String name) throws IOException {
    return FileUtils.readFileToString(
        new File(getSourceResourceFile(AppDynamicsCVConfig.class, "/appdynamics/dsl/" + name)), StandardCharsets.UTF_8);
  }

  private String readJson(String name) throws IOException {
    return FileUtils.readFileToString(
        new File(getResourceFilePath("hoverfly/appdynamics/" + name)), StandardCharsets.UTF_8);
  }
}
