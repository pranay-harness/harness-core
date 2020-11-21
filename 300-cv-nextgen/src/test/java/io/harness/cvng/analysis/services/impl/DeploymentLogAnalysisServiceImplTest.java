package io.harness.cvng.analysis.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterCoordinates;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId, null);

    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters_WithHostNameFilter() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));
    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId, "node2");
    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(1);
    assertThat(logAnalysisClusterChartDTOlist.get(0).getText()).isEqualTo("Error in cluster 2");
    assertThat(logAnalysisClusterChartDTOlist.get(0).getLabel()).isEqualTo(2);
    assertThat(logAnalysisClusterChartDTOlist.get(0).getX()).isEqualTo(0.4525);
    assertThat(logAnalysisClusterChartDTOlist.get(0).getY()).isEqualTo(0.542524);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0, null);

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
        deploymentLogAnalysisService.getLogAnalysisClusters(accountId, verificationJobInstanceId, null);
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
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, 1, 0, null);

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
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, 15, 0, null);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withHostNameFilter() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0, "node2");

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongHostNameFilter() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, 0, generateUuid());

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
    deploymentLogAnalysis2.setClusters(Arrays.asList(createCluster("Error in cluster 4", 4)));
    ClusterSummary clusterSummary = createClusterSummary(0, 0, 0, 4, null, null, ClusterType.KNOWN_EVENT);
    deploymentLogAnalysis2.setResultSummary(createResultSummary(0, 0, Arrays.asList(4), Arrays.asList(clusterSummary)));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis2);

    PageResponse<LogAnalysisClusterDTO> pageResponse =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0, null);

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
    List<Cluster> clusters = new ArrayList();
    List<ClusterSummary> clusterSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      clusters.add(createCluster("Cluster " + i, i));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null, null, ClusterType.KNOWN_EVENT));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, 0, null, clusterSummaries));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageResponse<LogAnalysisClusterDTO> pageResponse1 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0, null);

    assertThat(pageResponse1.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse1.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse1.getContent()).isNotNull();
    assertThat(pageResponse1.getContent().size()).isEqualTo(10);

    PageResponse<LogAnalysisClusterDTO> pageResponse2 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 1, null);

    assertThat(pageResponse2.getPageIndex()).isEqualTo(1);
    assertThat(pageResponse2.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse2.getContent()).isNotNull();
    assertThat(pageResponse2.getContent().size()).isEqualTo(10);

    PageResponse<LogAnalysisClusterDTO> pageResponse3 =
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 2, null);

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
        deploymentLogAnalysisService.getLogAnalysisResult(accountId, verificationJobInstanceId, null, 0, null);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongVerificationJobInstanceId() {
    assertThatThrownBy(
        () -> deploymentLogAnalysisService.getLogAnalysisResult(accountId, generateUuid(), null, 0, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No verification task mapping exist for verificationJobInstanceId");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_noData() {
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    assertThat(deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId))
        .isEqualTo(Optional.empty());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_getLatestData() {
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    List<ClusterSummary> clusterSummaries = new ArrayList();
    List<Cluster> clusters = new ArrayList();
    for (int i = 0; i < 25; i++) {
      //      clusters.add(createCluster("Cluster " + i, i, 0, 0));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null, null, ClusterType.KNOWN_EVENT));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, .7654, null, clusterSummaries));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    assertThat(deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId).get())
        .isCloseTo(.7654, offset(.0001));
  }

  private DeploymentLogAnalysis createDeploymentLogAnalysis(String verificationTaskId) {
    Cluster cluster1 = createCluster("Error in cluster 1", 1);
    Cluster cluster2 = createCluster("Error in cluster 2", 2);
    Cluster cluster3 = createCluster("Error in cluster 3", 3);
    List<Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3);

    ClusterCoordinates clusterCoordinates1 = createClusterCoordinates("node1", 1, 0.6464, 0.717171);
    ClusterCoordinates clusterCoordinates2 = createClusterCoordinates("node2", 2, 0.4525, 0.542524);
    ClusterCoordinates clusterCoordinates3 = createClusterCoordinates("node3", 3, 0.2131, 0.4151);
    List<ClusterCoordinates> clusterCoordinatesList =
        Arrays.asList(clusterCoordinates1, clusterCoordinates2, clusterCoordinates3);

    ClusterSummary clusterSummary1 =
        createClusterSummary(1, 0.7, 36, 1, Arrays.asList(1D), Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary2 =
        createClusterSummary(0, 0, 3, 2, Arrays.asList(5D), Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary3 =
        createClusterSummary(2, 2.2, 55, 3, Arrays.asList(3D), Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary = createResultSummary(
        1, 1, Arrays.asList(1, 2), Arrays.asList(clusterSummary1, clusterSummary2, clusterSummary3));

    ClusterSummary clusterSummary4 =
        createClusterSummary(2, 0.7, 36, 1, Arrays.asList(1D), Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary5 =
        createClusterSummary(2, 0, 3, 2, Arrays.asList(5D), Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary6 =
        createClusterSummary(2, 2.2, 55, 3, Arrays.asList(3D), Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary2 = createResultSummary(
        2, 1, Arrays.asList(1, 2), Arrays.asList(clusterSummary4, clusterSummary5, clusterSummary6));

    HostSummary hostSummary1 = createHostSummary("node1", resultSummary);
    HostSummary hostSummary2 = createHostSummary("node2", resultSummary2);
    return DeploymentLogAnalysis.builder()
        .accountId(accountId)
        .clusters(clusters)
        .clusterCoordinates(clusterCoordinatesList)
        .verificationTaskId(verificationTaskId)
        .resultSummary(resultSummary)
        .hostSummaries(Arrays.asList(hostSummary1, hostSummary2))
        .startTime(Instant.now())
        .endTime(Instant.now().plus(10, ChronoUnit.MINUTES))
        .build();
  }

  private ResultSummary createResultSummary(
      int risk, double score, List<Integer> controlClusterLabels, List<ClusterSummary> testClusterSummaries) {
    return ResultSummary.builder()
        .risk(risk)
        .score(score)
        .controlClusterLabels(controlClusterLabels)
        .testClusterSummaries(testClusterSummaries)
        .build();
  }

  private ClusterCoordinates createClusterCoordinates(String hostName, int label, double x, double y) {
    return ClusterCoordinates.builder().host(hostName).label(label).x(x).y(y).build();
  }

  private Cluster createCluster(String text, int label) {
    return Cluster.builder().text(text).label(label).build();
  }

  private ClusterSummary createClusterSummary(int risk, double score, int count, int label,
      List<Double> controlFrequencyData, List<Double> testFrequencyData, ClusterType clusterType) {
    return ClusterSummary.builder()
        .risk(risk)
        .clusterType(clusterType)
        .score(score)
        .count(count)
        .label(label)
        .controlFrequencyData(controlFrequencyData)
        .testFrequencyData(testFrequencyData)
        .build();
  }

  private HostSummary createHostSummary(String host, ResultSummary resultSummary) {
    return HostSummary.builder().host(host).resultSummary(resultSummary).build();
  }
}
