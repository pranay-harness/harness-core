package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TransactionSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeploymentTimeSeriesAnalysisServiceImplTest extends CvNextGenTest {
  @Inject private HPersistence hPersistence;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;

  private String accountId;
  private String cvConfigId;
  private String deploymentVerificationTaskId;

  @Before
  public void setUp() {
    accountId = generateUuid();
    cvConfigId = generateUuid();
    deploymentVerificationTaskId = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withoutFiltering() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, null, 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().get(0).getTransactionName())
        .isEqualTo("/todolist/requestLogin");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameFilter() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, "node1", 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().get(0).getTransactionName())
        .isEqualTo("hostSummary/todolist/requestLogin");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withWrongHostNameFilter() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, "randomNode", 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withAnomalousMetricsFilter() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, true, null, 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().get(0).getTransactionName())
        .isEqualTo("/todolist/requestLogin");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withAnomalousMetricsFilter_whenRiskIsLow() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);

    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummaryWithLowRisk =
        createResultSummary(0, 0D, deploymentTimeSeriesAnalysis.getResultSummary().getTransactionSummaries());
    deploymentTimeSeriesAnalysis.setResultSummary(resultSummaryWithLowRisk);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, true, null, 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameAndAnomalousFilter_andRiskIsHigh() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, true, "node1", 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().get(0).getTransactionName())
        .isEqualTo("hostSummary/todolist/requestLogin");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameAndAnomalousFilter_andRiskIsLow() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);

    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummary = createResultSummary(0, 0D, Collections.emptyList());
    DeploymentTimeSeriesAnalysisDTO.HostSummary hostSummary = createHostSummary("node1", "false", resultSummary);
    deploymentTimeSeriesAnalysis.setHostSummaries(Arrays.asList(hostSummary));
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, true, "node1", 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(0);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultipleDeploymentTimeSeriesAnalises() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis2 = createDeploymentTimSeriesAnalysis(verificationTaskId);
    Instant analysisStartTime = Instant.now().plus(1, ChronoUnit.HOURS);
    deploymentTimeSeriesAnalysis2.setStartTime(analysisStartTime);

    DeploymentTimeSeriesAnalysisDTO.TransactionSummary transactionSummary =
        createTransactionSummary("expectedTransactionName",
            deploymentTimeSeriesAnalysis2.getResultSummary().getTransactionSummaries().get(0).getMetricSummaries());
    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummary =
        createResultSummary(2, 0D, Arrays.asList(transactionSummary));

    deploymentTimeSeriesAnalysis2.setResultSummary(resultSummary);
    hPersistence.save(deploymentTimeSeriesAnalysis);
    hPersistence.save(deploymentTimeSeriesAnalysis2);

    TransactionSummaryPageDTO transactionSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, null, 1);

    assertThat(transactionSummaryPageDTO.getPageNumber()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getNumberOfPages()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getElementRange().getToIndex()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries()).isNotNull();
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().size()).isEqualTo(1);
    assertThat(transactionSummaryPageDTO.getTransactionSummaries().get(0).getTransactionName())
        .isEqualTo("expectedTransactionName");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultiplePages() {
    VerificationTask verificationTask = createVerificationTask();

    String verificationTaskId = hPersistence.save(verificationTask);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    List<DeploymentTimeSeriesAnalysisDTO.MetricSummary> metricSummaries =
        Arrays.asList(createMetricSummary("metric", 0, 0D, Arrays.asList(0D), Arrays.asList(0D)));
    List<DeploymentTimeSeriesAnalysisDTO.TransactionSummary> transactionSummaries = new ArrayList();

    for (int i = 0; i < 25; i++) {
      transactionSummaries.add(createTransactionSummary("transaction" + i, metricSummaries));
    }
    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummary = createResultSummary(0, 0D, transactionSummaries);
    deploymentTimeSeriesAnalysis.setResultSummary(resultSummary);
    hPersistence.save(deploymentTimeSeriesAnalysis);

    TransactionSummaryPageDTO page1 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, null, 1);

    assertThat(page1.getPageNumber()).isEqualTo(1);
    assertThat(page1.getNumberOfPages()).isEqualTo(3);
    assertThat(page1.getElementRange().getFromIndex()).isEqualTo(1);
    assertThat(page1.getElementRange().getToIndex()).isEqualTo(10);
    assertThat(page1.getTransactionSummaries()).isNotNull();
    assertThat(page1.getTransactionSummaries().size()).isEqualTo(10);

    TransactionSummaryPageDTO page2 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, null, 2);

    assertThat(page2.getPageNumber()).isEqualTo(2);
    assertThat(page2.getNumberOfPages()).isEqualTo(3);
    assertThat(page2.getElementRange().getFromIndex()).isEqualTo(11);
    assertThat(page2.getElementRange().getToIndex()).isEqualTo(20);
    assertThat(page2.getTransactionSummaries()).isNotNull();
    assertThat(page2.getTransactionSummaries().size()).isEqualTo(10);

    TransactionSummaryPageDTO page3 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, deploymentVerificationTaskId, false, null, 3);

    assertThat(page3.getPageNumber()).isEqualTo(3);
    assertThat(page3.getNumberOfPages()).isEqualTo(3);
    assertThat(page3.getElementRange().getFromIndex()).isEqualTo(21);
    assertThat(page3.getElementRange().getToIndex()).isEqualTo(25);
    assertThat(page3.getTransactionSummaries()).isNotNull();
    assertThat(page3.getTransactionSummaries().size()).isEqualTo(5);
  }

  private VerificationTask createVerificationTask() {
    return VerificationTask.builder()
        .cvConfigId(cvConfigId)
        .accountId(accountId)
        .deploymentVerificationTaskId(deploymentVerificationTaskId)
        .build();
  }
  private DeploymentTimeSeriesAnalysisDTO.ResultSummary createResultSummary(
      int risk, Double score, List<DeploymentTimeSeriesAnalysisDTO.TransactionSummary> transactionSummaries) {
    return DeploymentTimeSeriesAnalysisDTO.ResultSummary.builder()
        .risk(risk)
        .score(score)
        .transactionSummaries(transactionSummaries)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.TransactionSummary createTransactionSummary(
      String transactionName, List<DeploymentTimeSeriesAnalysisDTO.MetricSummary> metricSummaries) {
    return DeploymentTimeSeriesAnalysisDTO.TransactionSummary.builder()
        .transactionName(transactionName)
        .metricSummaries(metricSummaries)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.MetricSummary createMetricSummary(
      String metricName, int risk, Double score, List<Double> testData, List<Double> controlData) {
    return DeploymentTimeSeriesAnalysisDTO.MetricSummary.builder()
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .testData(testData)
        .controlData(controlData)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.HostSummary createHostSummary(
      String hostName, String isNewHost, DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummary) {
    return DeploymentTimeSeriesAnalysisDTO.HostSummary.builder()
        .hostName(hostName)
        .isNewHost(isNewHost)
        .resultSummary(resultSummary)
        .build();
  }

  private DeploymentTimeSeriesAnalysis createDeploymentTimSeriesAnalysis(String verificationTaskId) {
    DeploymentTimeSeriesAnalysisDTO.MetricSummary callsPerMinute =
        createMetricSummary("Calls per Minute", -1, 0D, Arrays.asList(2D), Arrays.asList(2D));

    DeploymentTimeSeriesAnalysisDTO.MetricSummary averageResponseTime =
        createMetricSummary("Average Response Time (ms)", 1, 0D, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionSummary transactionSummary =
        createTransactionSummary("/todolist/requestLogin", Arrays.asList(callsPerMinute, averageResponseTime));

    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummary =
        createResultSummary(2, 1.64D, Arrays.asList(transactionSummary));

    DeploymentTimeSeriesAnalysisDTO.MetricSummary callsPerMinute2 =
        createMetricSummary("Calls per Minute", 0, 0D, Arrays.asList(3D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.MetricSummary averageResponseTime2 =
        createMetricSummary("Average Response Time (ms)", 2, 1D, Arrays.asList(0D), Arrays.asList(0D));

    DeploymentTimeSeriesAnalysisDTO.TransactionSummary transactionSummary2 = createTransactionSummary(
        "hostSummary/todolist/requestLogin", Arrays.asList(callsPerMinute2, averageResponseTime2));

    DeploymentTimeSeriesAnalysisDTO.ResultSummary resultSummaryForHost =
        createResultSummary(1, 1D, Arrays.asList(transactionSummary2));

    DeploymentTimeSeriesAnalysisDTO.HostSummary hostSummary = createHostSummary("node1", "false", resultSummaryForHost);

    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .verificationTaskId(verificationTaskId)
        .resultSummary(resultSummary)
        .hostSummaries(Arrays.asList(hostSummary))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }
}