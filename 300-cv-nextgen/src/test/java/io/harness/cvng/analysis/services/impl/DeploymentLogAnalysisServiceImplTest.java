package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeploymentLogAnalysisServiceImplTest extends CvNextGenTest {
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  private String accountId;
  private String cvConfigId;
  private String verificationJobInstanceId;

  @Before
  public void setUp() {
    accountId = generateUuid();
    cvConfigId = generateUuid();
    verificationJobInstanceId = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));
    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId);

    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);
  }
  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters_withNoDeploymentLogAnalysis() {
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList =
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId);
    assertThat(logAnalysisClusterChartDTOList).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withLabelFilter() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, 1, 0);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getScore()).isEqualTo(0.7);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongLabel() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, 15, 0);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withMultipleLogAnalyses() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    DeploymentLogAnalysis deploymentLogAnalysis2 = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysis2.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
    deploymentLogAnalysis2.setClusters(Arrays.asList(createCluster("Error in cluster 4", 4, 0.2671, 0.971)));
    DeploymentLogAnalysisDTO.ClusterSummary clusterSummary = createClusterSummary(0, 0, 0, 4, null, null);
    deploymentLogAnalysis2.setResultSummary(createResultSummary(0, 0, Arrays.asList(4), Arrays.asList(clusterSummary)));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis2);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(4);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withMultiplePages() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    List<DeploymentLogAnalysisDTO.Cluster> clusters = new ArrayList();
    List<DeploymentLogAnalysisDTO.ClusterSummary> clusterSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      clusters.add(createCluster("Cluster " + i, i, 0, 0));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null, null));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, 0, null, clusterSummaries));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse1 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0);

    assertThat(pageResponse1.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse1.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse1.getContent()).isNotNull();
    assertThat(pageResponse1.getContent().size()).isEqualTo(10);

    PageResponse<LogAnalysisClusterDTO> pageResponse2 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 1);

    assertThat(pageResponse2.getPageIndex()).isEqualTo(1);
    assertThat(pageResponse2.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse2.getContent()).isNotNull();
    assertThat(pageResponse2.getContent().size()).isEqualTo(10);

    PageResponse<LogAnalysisClusterDTO> pageResponse3 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 2);

    assertThat(pageResponse3.getPageIndex()).isEqualTo(2);
    assertThat(pageResponse3.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse3.getContent()).isNotNull();
    assertThat(pageResponse3.getContent().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withoutDeploymentLogAnalysis() {
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongVerificationJobInstanceId() {
    assertThatThrownBy(() -> deploymentLogAnalysisService.getLogAnalysisResult(accountId, generateUuid(), null, 0))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No verification task mapping exist for verificationJobInstanceId");
  }

  private DeploymentLogAnalysis createDeploymentLogAnalysis(String verificationTaskId) {
    DeploymentLogAnalysisDTO.Cluster cluster1 = createCluster("Error in cluster 1", 1, 0.6464, 0.717171);
    DeploymentLogAnalysisDTO.Cluster cluster2 = createCluster("Error in cluster 2", 2, 0.4525, 0.542524);
    DeploymentLogAnalysisDTO.Cluster cluster3 = createCluster("Error in cluster 3", 3, 0.4525, 0.542524);
    List<DeploymentLogAnalysisDTO.Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3);

    DeploymentLogAnalysisDTO.ClusterSummary clusterSummary1 =
        createClusterSummary(1, 0.7, 36, 1, Arrays.asList(1D), Arrays.asList(2D));

    DeploymentLogAnalysisDTO.ClusterSummary clusterSummary2 =
        createClusterSummary(0, 0, 3, 2, Arrays.asList(5D), Arrays.asList(2D));

    DeploymentLogAnalysisDTO.ClusterSummary clusterSummary3 =
        createClusterSummary(2, 2.2, 55, 3, Arrays.asList(3D), Arrays.asList(4D));

    DeploymentLogAnalysisDTO.ResultSummary resultSummary = createResultSummary(
        1, 1, Arrays.asList(1, 2), Arrays.asList(clusterSummary1, clusterSummary2, clusterSummary3));

    DeploymentLogAnalysisDTO.HostSummary hostSummary = createHostSummary("host1", resultSummary);

    return DeploymentLogAnalysis.builder()
        .accountId(accountId)
        .clusters(clusters)
        .verificationTaskId(verificationTaskId)
        .resultSummary(resultSummary)
        .hostSummaries(Arrays.asList(hostSummary))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(10, ChronoUnit.MINUTES))
        .build();
  }

  private DeploymentLogAnalysisDTO.ResultSummary createResultSummary(int risk, double score,
      List<Integer> controlClusterLabels, List<DeploymentLogAnalysisDTO.ClusterSummary> testClusterSummaries) {
    return DeploymentLogAnalysisDTO.ResultSummary.builder()
        .risk(risk)
        .score(score)
        .controlClusterLabels(controlClusterLabels)
        .testClusterSummaries(testClusterSummaries)
        .build();
  }

  private DeploymentLogAnalysisDTO.Cluster createCluster(String text, int label, double x, double y) {
    return DeploymentLogAnalysisDTO.Cluster.builder().text(text).label(label).x(x).y(y).build();
  }

  private DeploymentLogAnalysisDTO.ClusterSummary createClusterSummary(
      int risk, double score, int count, int label, List<Double> controlFrequencyData, List<Double> testFrequencyData) {
    return DeploymentLogAnalysisDTO.ClusterSummary.builder()
        .risk(risk)
        .score(score)
        .count(count)
        .label(label)
        .controlFrequencyData(controlFrequencyData)
        .testFrequencyData(testFrequencyData)
        .build();
  }

  private DeploymentLogAnalysisDTO.HostSummary createHostSummary(
      String host, DeploymentLogAnalysisDTO.ResultSummary resultSummary) {
    return DeploymentLogAnalysisDTO.HostSummary.builder().host(host).resultSummary(resultSummary).build();
  }
}