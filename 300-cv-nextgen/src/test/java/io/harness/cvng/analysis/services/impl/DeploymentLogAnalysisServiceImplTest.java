/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.analysis.services.impl;

import static io.harness.cvng.beans.DataSourceType.APP_DYNAMICS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.LogsAnalysisSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.Cluster;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterCoordinates;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ControlClusterSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ResultSummary;
import io.harness.cvng.analysis.beans.LogAnalysisClusterChartDTO;
import io.harness.cvng.analysis.beans.LogAnalysisClusterDTO;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.services.api.DeploymentLogAnalysisService;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.filterParams.DeploymentLogAnalysisFilter;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.ng.beans.PageResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeploymentLogAnalysisServiceImplTest extends CvNextGenTestBase {
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private DeploymentLogAnalysisService deploymentLogAnalysisService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;

  private String accountId;
  private String cvConfigId;
  private String verificationJobInstanceId;
  BuilderFactory builderFactory;

  @Before
  public void setUp() {
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    VerificationJobInstance verificationJobInstance = builderFactory.verificationJobInstanceBuilder().build();
    cvConfigId =
        verificationJobInstance.getCvConfigMap().values().stream().collect(Collectors.toList()).get(0).getUuid();
    verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(
            accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);

    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClustersWithClusterTypeFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
                                                                  .hostName(null)
                                                                  .build();

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(
            accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(null)
                                      .clusterTypes(Arrays.asList(ClusterType.UNKNOWN_EVENT))
                                      .hostName(null)
                                      .build();
    logAnalysisClusterChartDTOlist = deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClustersWithHealthIdentifierFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));

    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
                                                                  .hostName(null)
                                                                  .build();

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(
            accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist).isNotNull();
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);

    String cvConfigIdentifier = verificationJobInstanceService.get(Arrays.asList(verificationJobInstanceId))
                                    .get(0)
                                    .getCvConfigMap()
                                    .values()
                                    .stream()
                                    .collect(Collectors.toList())
                                    .get(0)
                                    .getIdentifier();

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(Arrays.asList(cvConfigIdentifier))
                                      .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
                                      .hostName(null)
                                      .build();
    logAnalysisClusterChartDTOlist = deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(Arrays.asList("some-random-identifier"))
                                      .clusterTypes(Arrays.asList(ClusterType.UNKNOWN_EVENT))
                                      .hostName(null)
                                      .build();
    logAnalysisClusterChartDTOlist = deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(0);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(Arrays.asList(cvConfigIdentifier))
                                      .clusterTypes(null)
                                      .hostName(null)
                                      .build();
    logAnalysisClusterChartDTOlist = deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(3);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(Arrays.asList(cvConfigIdentifier))
                                      .clusterTypes(Arrays.asList(ClusterType.UNKNOWN_EVENT))
                                      .hostName(null)
                                      .build();
    logAnalysisClusterChartDTOlist = deploymentLogAnalysisService.getLogAnalysisClusters(
        accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOlist.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters_WithHostNameFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    deploymentLogAnalysisService.save(createDeploymentLogAnalysis(verificationTaskId));

    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(null)
                                                                  .hostName("node2")
                                                                  .build();

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOlist =
        deploymentLogAnalysisService.getLogAnalysisClusters(
            accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
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
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();

    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(3);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResultWithClusterFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();

    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
                                                                  .hostName(null)
                                                                  .build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(3);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(null)
                                      .clusterTypes(Arrays.asList(ClusterType.UNEXPECTED_FREQUENCY))
                                      .hostName(null)
                                      .build();
    pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getContent().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResultWithHealthSourceIdentifierFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();

    String cvConfigIdentifier = verificationJobInstanceService.get(Arrays.asList(verificationJobInstanceId))
                                    .get(0)
                                    .getCvConfigMap()
                                    .values()
                                    .stream()
                                    .collect(Collectors.toList())
                                    .get(0)
                                    .getIdentifier();

    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder()
            .healthSourceIdentifiers(Arrays.asList(cvConfigIdentifier))
            .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
            .hostName(null)
            .build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(3);

    deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                      .healthSourceIdentifiers(Arrays.asList("some-identifier"))
                                      .clusterTypes(Arrays.asList(ClusterType.KNOWN_EVENT))
                                      .hostName(null)
                                      .build();
    pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getContent().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisClusters_withNoDeploymentLogAnalysis() {
    verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();

    List<LogAnalysisClusterChartDTO> logAnalysisClusterChartDTOList =
        deploymentLogAnalysisService.getLogAnalysisClusters(
            accountId, verificationJobInstanceId, deploymentLogAnalysisFilter);
    assertThat(logAnalysisClusterChartDTOList).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withLabelFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, 1, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getScore()).isEqualTo(0.7);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongLabel() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);

    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();

    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, 15, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withHostNameFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(null)
                                                                  .hostName("node2")
                                                                  .build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongHostNameFilter() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter = DeploymentLogAnalysisFilter.builder()
                                                                  .healthSourceIdentifiers(null)
                                                                  .clusterTypes(null)
                                                                  .hostName(generateUuid())
                                                                  .build();

    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withMultipleLogAnalyses() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    DeploymentLogAnalysis deploymentLogAnalysis2 = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysis2.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
    deploymentLogAnalysis2.setClusters(Arrays.asList(createCluster("Error in cluster 4", 4)));
    ClusterSummary clusterSummary = createClusterSummary(0, 0, 0, 4, null, ClusterType.KNOWN_EVENT);
    deploymentLogAnalysis2.setResultSummary(createResultSummary(0, 0, Arrays.asList(clusterSummary), null));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    deploymentLogAnalysisService.save(deploymentLogAnalysis2);

    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent()).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getLabel()).isEqualTo(4);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withMultiplePages() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    List<Cluster> clusters = new ArrayList();
    List<ClusterSummary> clusterSummaries = new ArrayList();
    for (int i = 0; i < 25; i++) {
      clusters.add(createCluster("Cluster " + i, i));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null, ClusterType.KNOWN_EVENT));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, 0, clusterSummaries, null));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse1 = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse1.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse1.getTotalPages()).isEqualTo(3);
    assertThat(pageResponse1.getContent()).isNotNull();
    assertThat(pageResponse1.getContent().size()).isEqualTo(10);

    pageParams = PageParams.builder().page(1).size(10).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse2 = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse2.getPageIndex()).isEqualTo(1);
    assertThat(pageResponse2.getTotalPages()).isEqualTo(3);
    assertThat(pageResponse2.getContent()).isNotNull();
    assertThat(pageResponse2.getContent().size()).isEqualTo(10);

    pageParams = PageParams.builder().page(2).size(10).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse3 = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse3.getPageIndex()).isEqualTo(2);
    assertThat(pageResponse3.getTotalPages()).isEqualTo(3);
    assertThat(pageResponse3.getContent()).isNotNull();
    assertThat(pageResponse3.getContent().size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withoutDeploymentLogAnalysis() {
    verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, verificationJobInstanceId, null, deploymentLogAnalysisFilter, pageParams);

    assertThat(pageResponse.getPageIndex()).isEqualTo(0);
    assertThat(pageResponse.getTotalPages()).isEqualTo(0);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetLogAnalysisResult_withWrongVerificationJobInstanceId() {
    PageParams pageParams = PageParams.builder().page(0).size(10).build();
    DeploymentLogAnalysisFilter deploymentLogAnalysisFilter =
        DeploymentLogAnalysisFilter.builder().healthSourceIdentifiers(null).clusterTypes(null).hostName(null).build();
    PageResponse<LogAnalysisClusterDTO> pageResponse = deploymentLogAnalysisService.getLogAnalysisResult(
        accountId, generateUuid(), null, deploymentLogAnalysisFilter, pageParams);
    assertThat(pageResponse.getContent()).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_noData() {
    verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    assertThat(deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId))
        .isEqualTo(Optional.empty());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_verificationTaskIdsDoesNotExists() {
    assertThatThrownBy(
        () -> deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRecentHighestRiskScore_getLatestData() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    List<ClusterSummary> clusterSummaries = new ArrayList();
    List<Cluster> clusters = new ArrayList();
    for (int i = 0; i < 25; i++) {
      //      clusters.add(createCluster("Cluster " + i, i, 0, 0));
      clusterSummaries.add(createClusterSummary(0, 0, 0, i, null, ClusterType.KNOWN_EVENT));
    }
    deploymentLogAnalysis.setClusters(clusters);
    deploymentLogAnalysis.setResultSummary(createResultSummary(0, .7654, clusterSummaries, null));
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    assertThat(deploymentLogAnalysisService.getRecentHighestRiskScore(accountId, verificationJobInstanceId).get())
        .isEqualTo(Risk.HEALTHY);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    DeploymentLogAnalysis deploymentLogAnalysis = createDeploymentLogAnalysis(verificationTaskId);
    deploymentLogAnalysisService.save(deploymentLogAnalysis);
    LogsAnalysisSummary summary =
        deploymentLogAnalysisService.getAnalysisSummary(accountId, Arrays.asList(verificationJobInstanceId));
    assertThat(summary).isNotNull();
    assertThat(summary.getAnomalousClusterCount()).isEqualTo(2);
    assertThat(summary.getTotalClusterCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary_noAnalysisYet() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    LogsAnalysisSummary summary =
        deploymentLogAnalysisService.getAnalysisSummary(accountId, Arrays.asList(verificationJobInstanceId));
    assertThat(summary).isNotNull();
    assertThat(summary.getAnomalousClusterCount()).isEqualTo(0);
    assertThat(summary.getTotalClusterCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetAnalysisSummary_nullInput() {
    String verificationTaskId = verificationTaskService.createDeploymentVerificationTask(
        accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);

    assertThatThrownBy(() -> deploymentLogAnalysisService.getAnalysisSummary(accountId, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Missing verificationJobInstanceIds when looking for summary");
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

    ClusterSummary clusterSummary1 = createClusterSummary(1, 0.7, 36, 1, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary2 = createClusterSummary(0, 0, 3, 2, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary3 = createClusterSummary(2, 2.2, 55, 3, Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary =
        createResultSummary(1, 1, Arrays.asList(clusterSummary1, clusterSummary2, clusterSummary3), null);

    ClusterSummary clusterSummary4 = createClusterSummary(2, 0.7, 36, 1, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary5 = createClusterSummary(2, 0, 3, 2, Arrays.asList(2D), ClusterType.KNOWN_EVENT);
    ClusterSummary clusterSummary6 = createClusterSummary(2, 2.2, 55, 3, Arrays.asList(4D), ClusterType.KNOWN_EVENT);

    ResultSummary resultSummary2 =
        createResultSummary(2, 1, Arrays.asList(clusterSummary4, clusterSummary5, clusterSummary6), null);

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

  private ResultSummary createResultSummary(int risk, double score, List<ClusterSummary> testClusterSummaries,
      List<ControlClusterSummary> controlClusterSummaries) {
    return ResultSummary.builder()
        .risk(risk)
        .score(score)
        .controlClusterSummaries(controlClusterSummaries)
        .testClusterSummaries(testClusterSummaries)
        .build();
  }

  private ClusterCoordinates createClusterCoordinates(String hostName, int label, double x, double y) {
    return ClusterCoordinates.builder().host(hostName).label(label).x(x).y(y).build();
  }

  private Cluster createCluster(String text, int label) {
    return Cluster.builder().text(text).label(label).build();
  }

  private ClusterSummary createClusterSummary(
      int risk, double score, int count, int label, List<Double> testFrequencyData, ClusterType clusterType) {
    return ClusterSummary.builder()
        .risk(risk)
        .clusterType(clusterType)
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
