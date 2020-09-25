package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.cvng.analysis.beans.TransactionMetricInfoSummaryPageDTO;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentTimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.VerificationJobInstanceDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.BadRequestException;

public class DeploymentTimeSeriesAnalysisServiceImplTest extends CvNextGenTest {
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentTimeSeriesAnalysisService deploymentTimeSeriesAnalysisService;

  private String accountId;
  private String cvConfigId;
  private String identifier;
  private String serviceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String envIdentifier;

  @Before
  public void setUp() {
    accountId = generateUuid();
    cvConfigId = generateUuid();
    serviceIdentifier = generateUuid();
    identifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    envIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getTransactionMetric()
                   .getTransactionName())
        .isEqualTo("/todolist/exception");
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getTransactionMetric().getScore())
        .isEqualTo(2.5); // ensures that sorting based on score from transaction works
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().size())
        .isEqualTo(2);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().first().getHostName())
        .isEqualTo("node2");
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().first().getScore())
        .isEqualTo(2); // checks that sorting per node works correctly
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameFilter() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, "node1", 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().size())
        .isEqualTo(1);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().first().getHostName())
        .isEqualTo("node1");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withWrongHostName() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));
    assertThatThrownBy(()
                           -> deploymentTimeSeriesAnalysisService.getMetrics(
                               accountId, verificationJobInstanceId, false, "randomNode", 0))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Host Name randomNode doesn't exist");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withAnomalousMetricsFilter() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, true, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getTransactionMetric().getScore())
        .isEqualTo(2.5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withHostNameAndAnomalousMetricsFilter() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, true, "node2", 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getTransactionMetric().getScore())
        .isEqualTo(2.5);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().size())
        .isEqualTo(1);
    assertThat(
        transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().get(0).getNodes().first().getHostName())
        .isEqualTo("node2");
  }
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultipleDeploymentTimeSeriesAnalyses() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentTimeSeriesAnalysisService.save(createDeploymentTimSeriesAnalysis(verificationTaskId));

    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    deploymentTimeSeriesAnalysis.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData =
        createTransactionMetricHostData("newTransaction", "newMetric", 5, 5.0,
            deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().get(0).getHostData());
    deploymentTimeSeriesAnalysis.setTransactionMetricSummaries(Arrays.asList(transactionMetricHostData));
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isNotNull();
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent().size()).isEqualTo(1);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse()
                   .getContent()
                   .get(0)
                   .getTransactionMetric()
                   .getTransactionName())
        .isEqualTo("newTransaction");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withMultiplePages() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    DeploymentTimeSeriesAnalysis deploymentTimeSeriesAnalysis = createDeploymentTimSeriesAnalysis(verificationTaskId);
    List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> transactionSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      transactionSummaries.add(createTransactionMetricHostData("transaction " + i, "metric", 0, 0.0,
          deploymentTimeSeriesAnalysis.getTransactionMetricSummaries().get(0).getHostData()));
    }
    deploymentTimeSeriesAnalysis.setTransactionMetricSummaries(transactionSummaries);
    deploymentTimeSeriesAnalysisService.save(deploymentTimeSeriesAnalysis);

    TransactionMetricInfoSummaryPageDTO page1 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 0);

    assertThat(page1.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(page1.getPageResponse().getPageCount()).isEqualTo(2);
    assertThat(page1.getPageResponse().getContent()).isNotNull();
    assertThat(page1.getPageResponse().getContent().size()).isEqualTo(10);

    TransactionMetricInfoSummaryPageDTO page2 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 1);

    assertThat(page2.getPageResponse().getPageIndex()).isEqualTo(1);
    assertThat(page2.getPageResponse().getPageCount()).isEqualTo(2);
    assertThat(page2.getPageResponse().getContent()).isNotNull();
    assertThat(page2.getPageResponse().getContent().size()).isEqualTo(10);

    TransactionMetricInfoSummaryPageDTO page3 =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 2);

    assertThat(page3.getPageResponse().getPageIndex()).isEqualTo(2);
    assertThat(page3.getPageResponse().getPageCount()).isEqualTo(2);
    assertThat(page3.getPageResponse().getContent()).isNotNull();
    assertThat(page3.getPageResponse().getContent().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetMetrics_withoutDeploymentTimeSeriesAnalysis() {
    verificationJobService.upsert(accountId, createCanaryVerificationJobDTO());
    String verificationJobInstanceId =
        verificationJobInstanceService.create(accountId, createVerificationJobInstanceDTO());
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    TransactionMetricInfoSummaryPageDTO transactionMetricInfoSummaryPageDTO =
        deploymentTimeSeriesAnalysisService.getMetrics(accountId, verificationJobInstanceId, false, null, 0);

    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageIndex()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getPageCount()).isEqualTo(0);
    assertThat(transactionMetricInfoSummaryPageDTO.getPageResponse().getContent()).isEmpty();
  }

  private VerificationJobInstanceDTO createVerificationJobInstanceDTO() {
    return VerificationJobInstanceDTO.builder()
        .verificationJobIdentifier(identifier)
        .verificationTaskStartTimeMs(Instant.now().toEpochMilli())
        .build();
  }

  private CanaryVerificationJobDTO createCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(identifier);
    canaryVerificationJobDTO.setJobName("jobName");
    canaryVerificationJobDTO.setDuration("100");
    canaryVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier(envIdentifier);
    canaryVerificationJobDTO.setDataSources(Arrays.asList(DataSourceType.APP_DYNAMICS));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.LOW.name());
    return canaryVerificationJobDTO;
  }

  private DeploymentTimeSeriesAnalysisDTO.HostInfo createHostInfo(
      String hostName, int risk, Double score, boolean presentBeforeDeployment, boolean presentAfterDeploymeent) {
    return DeploymentTimeSeriesAnalysisDTO.HostInfo.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .presentAfterDeployment(presentBeforeDeployment)
        .presentAfterDeployment(presentAfterDeploymeent)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.HostData createHostData(
      String hostName, int risk, Double score, List<Double> controlData, List<Double> testData) {
    return DeploymentTimeSeriesAnalysisDTO.HostData.builder()
        .hostName(hostName)
        .risk(risk)
        .score(score)
        .controlData(controlData)
        .testData(testData)
        .build();
  }

  private DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData createTransactionMetricHostData(
      String transactionName, String metricName, int risk, Double score,
      List<DeploymentTimeSeriesAnalysisDTO.HostData> hostDataList) {
    return DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData.builder()
        .transactionName(transactionName)
        .metricName(metricName)
        .risk(risk)
        .score(score)
        .hostData(hostDataList)
        .build();
  }

  private DeploymentTimeSeriesAnalysis createDeploymentTimSeriesAnalysis(String verificationTaskId) {
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo1 = createHostInfo("node1", 1, 1.1, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostInfo hostInfo2 = createHostInfo("node2", 2, 2.2, false, true);
    DeploymentTimeSeriesAnalysisDTO.HostData hostData1 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData2 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData1 =
        createTransactionMetricHostData(
            "/todolist/inside", "Errors per Minute", 0, 0.5, Arrays.asList(hostData1, hostData2));

    DeploymentTimeSeriesAnalysisDTO.HostData hostData3 =
        createHostData("node1", 0, 0.0, Arrays.asList(1D), Arrays.asList(1D));
    DeploymentTimeSeriesAnalysisDTO.HostData hostData4 =
        createHostData("node2", 2, 2.0, Arrays.asList(1D), Arrays.asList(1D));

    DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData transactionMetricHostData2 =
        createTransactionMetricHostData(
            "/todolist/exception", "Calls per Minute", 2, 2.5, Arrays.asList(hostData3, hostData4));

    return DeploymentTimeSeriesAnalysis.builder()
        .accountId(accountId)
        .verificationTaskId(verificationTaskId)
        .transactionMetricSummaries(Arrays.asList(transactionMetricHostData1, transactionMetricHostData2))
        .hostSummaries(Arrays.asList(hostInfo1, hostInfo2))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(1, ChronoUnit.MINUTES))
        .build();
  }
}