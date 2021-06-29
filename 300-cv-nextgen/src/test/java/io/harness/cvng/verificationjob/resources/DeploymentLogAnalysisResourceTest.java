package io.harness.cvng.verificationjob.resources;

import static io.harness.cvng.beans.DataSourceType.APP_DYNAMICS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ControlClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.ng.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.ResourceTestRule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeploymentLogAnalysisResourceTest extends CvNextGenTestBase {
  @Inject private Injector injector;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;

  @Inject
  private static DeploymentLogAnalysisResource deploymentLogAnalysisResource = new DeploymentLogAnalysisResource();

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(deploymentLogAnalysisResource).build();

  private String accountId;
  private String cvConfigId;
  private String verificationJobInstanceId;

  @Before
  public void setUp() {
    injector.injectMembers(deploymentLogAnalysisResource);
    this.accountId = generateUuid();
    this.cvConfigId = generateUuid();
    this.verificationJobInstanceId = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult() {
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/deployment-log-analysis")
                            .path(verificationJobInstanceId)
                            .queryParam("accountId", accountId)
                            .queryParam("pageNumber", 0)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        response.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withLabelFilter() {
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/deployment-log-analysis")
                            .path(verificationJobInstanceId)
                            .queryParam("accountId", accountId)
                            .queryParam("pageNumber", 0)
                            .queryParam("label", 1)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        response.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

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
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/deployment-log-analysis")
                            .path(verificationJobInstanceId)
                            .queryParam("accountId", accountId)
                            .queryParam("pageNumber", 0)
                            .queryParam("label", 15)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        response.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withMultipleLogAnalyses() {
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    DeploymentLogAnalysis deploymentLogAnalysis2 = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysis2.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
    deploymentLogAnalysis2.setClusters(Arrays.asList(createCluster("Error in cluster 4", 4, 0.2671, 0.971)));
    ClusterSummary clusterSummary = createClusterSummary(0, 0, 0, 4, null);
    deploymentLogAnalysis2.setResultSummary(createResultSummary(0, 0, Arrays.asList(clusterSummary), null));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis2);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/deployment-log-analysis")
                            .path(verificationJobInstanceId)
                            .queryParam("accountId", accountId)
                            .queryParam("pageNumber", 0)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        response.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

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
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    List<Cluster> clusters = new ArrayList();
    List<ClusterSummary> clusterSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      clusters.add(createCluster("Cluster " + i, i, 0, 0));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, 0, clusterSummaries, null));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    Response response1 = RESOURCES.client()
                             .target("http://localhost:9998/deployment-log-analysis")
                             .path(verificationJobInstanceId)
                             .queryParam("accountId", accountId)
                             .queryParam("pageNumber", 0)
                             .request(MediaType.APPLICATION_JSON_TYPE)
                             .get();

    assertThat(response1.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse1 =
        response1.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse1.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse1.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse1.getContent()).isNotNull();
    assertThat(pageResponse1.getContent().size()).isEqualTo(10);

    Response response2 = RESOURCES.client()
                             .target("http://localhost:9998/deployment-log-analysis")
                             .path(verificationJobInstanceId)
                             .queryParam("accountId", accountId)
                             .queryParam("pageNumber", 1)
                             .request(MediaType.APPLICATION_JSON_TYPE)
                             .get();

    assertThat(response2.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse2 =
        response2.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse2.getPageIndex()).isEqualTo(1);
    assertThat(pageResponse2.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse2.getContent()).isNotNull();
    assertThat(pageResponse2.getContent().size()).isEqualTo(10);

    Response response3 = RESOURCES.client()
                             .target("http://localhost:9998/deployment-log-analysis")
                             .path(verificationJobInstanceId)
                             .queryParam("accountId", accountId)
                             .queryParam("pageNumber", 2)
                             .request(MediaType.APPLICATION_JSON_TYPE)
                             .get();

    assertThat(response2.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse3 =
        response3.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse3.getPageIndex()).isEqualTo(2);
    assertThat(pageResponse3.getTotalPages()).isEqualTo(2);
    assertThat(pageResponse3.getContent()).isNotNull();
    assertThat(pageResponse3.getContent().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withoutDeploymentLogAnalysis() {
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/deployment-log-analysis")
                            .path(verificationJobInstanceId)
                            .queryParam("accountId", accountId)
                            .queryParam("pageNumber", 0)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    PageResponse<LogAnalysisClusterDTO> pageResponse =
        response.readEntity(new GenericType<RestResponse<PageResponse<LogAnalysisClusterDTO>>>() {}).getResource();

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  private DeploymentLogAnalysis createDeploymentLogAnalysis(String verificationTaskId) {
    Cluster cluster1 = createCluster("Error in cluster 1", 1, 0.6464, 0.717171);
    Cluster cluster2 = createCluster("Error in cluster 2", 2, 0.4525, 0.542524);
    Cluster cluster3 = createCluster("Error in cluster 3", 3, 0.4525, 0.542524);
    List<Cluster> clusters = Arrays.asList(cluster1, cluster2, cluster3);

    ClusterSummary clusterSummary1 = createClusterSummary(1, 0.7, 36, 1, Arrays.asList(1D));

    ClusterSummary clusterSummary2 = createClusterSummary(0, 0, 3, 2, Arrays.asList(5D));

    ClusterSummary clusterSummary3 = createClusterSummary(2, 2.2, 55, 3, Arrays.asList(3D));

    ResultSummary resultSummary =
        createResultSummary(1, 1, Arrays.asList(clusterSummary1, clusterSummary2, clusterSummary3), null);

    HostSummary hostSummary = createHostSummary("host1", resultSummary);

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

  private ResultSummary createResultSummary(int risk, double score, List<ClusterSummary> testClusterSummaries,
      List<ControlClusterSummary> controlClusterSummaries) {
    return ResultSummary.builder()
        .risk(risk)
        .score(score)
        .controlClusterSummaries(controlClusterSummaries)
        .testClusterSummaries(testClusterSummaries)
        .build();
  }

  private Cluster createCluster(String text, int label, double x, double y) {
    return Cluster.builder().text(text).label(label).build();
  }

  private ClusterSummary createClusterSummary(
      int risk, double score, int count, int label, List<Double> testFrequencyData) {
    return ClusterSummary.builder()
        .risk(risk)
        .score(score)
        .count(count)
        .label(label)
        .testFrequencyData(testFrequencyData)
        .build();
  }

  private HostSummary createHostSummary(String host, ResultSummary resultSummary) {
    return HostSummary.builder().host(host).resultSummary(resultSummary).build();
  }
}
