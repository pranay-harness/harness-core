package io.harness.cdng.pipeline.executions;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.cdng.pipeline.DeploymentStage.DEPLOYMENT_STAGE_TYPE;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.execution.NodeExecution;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.CDStageExecutionSummary.CDStageExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.DockerArtifactSummary;
import io.harness.ngpipeline.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.ngpipeline.pipeline.executions.beans.ServiceExecutionSummary;
import io.harness.ngpipeline.pipeline.executions.beans.StageExecutionSummary;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.groovy.util.Maps;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
public class PipelineExecutionHelperTest extends CDNGTestBase {
  @Inject PipelineExecutionHelper pipelineExecutionHelper;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testAddStageSpecificDetailsToPipelineExecution_StageElement() throws IOException {
    StageElement stageElement = StageElement.builder().identifier("testIdentifier").build();
    ServiceYaml entity = ServiceYaml.builder().identifier("serviceIdentifier").name("serviceName").build();
    DeploymentStage deploymentStage =
        DeploymentStage.builder()
            .identifier("testIdentifier")
            .serviceConfig(ServiceConfig.builder()
                               .service(entity)
                               .serviceDefinition(ServiceDefinition.builder().type("Kubernetes").build())
                               .build())
            .infrastructure(
                PipelineInfrastructure.builder()
                    .environment(EnvironmentYaml.builder().identifier("envIdentifier").name("stageName").build())
                    .build())
            .build();
    stageElement.setStageType(deploymentStage);
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    NgPipeline ngPipeline = NgPipeline.builder().stage(stageElement).build();
    pipelineExecutionHelper.addStageSpecificDetailsToPipelineExecution(
        pipelineExecutionSummary, ngPipeline, Maps.of("testIdentifier", "node1"));

    CDStageExecutionSummary executionSummary = CDStageExecutionSummary.builder()
                                                   .planNodeId("node1")
                                                   .stageIdentifier("testIdentifier")
                                                   .executionStatus(ExecutionStatus.NOT_STARTED)
                                                   .build();
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().size()).isEqualTo(1);
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().get(0)).isEqualTo(executionSummary);
    assertThat(pipelineExecutionSummary.getServiceIdentifiers()).isEqualTo(new ArrayList<>());
    assertThat(pipelineExecutionSummary.getStageIdentifiers()).isEqualTo(Lists.newArrayList("testIdentifier"));
    assertThat(pipelineExecutionSummary.getStageTypes()).isEqualTo(Lists.newArrayList(DEPLOYMENT_STAGE_TYPE));
    assertThat(pipelineExecutionSummary.getServiceDefinitionTypes()).isEqualTo(new ArrayList<>());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testAddStageSpecificDetailsToPipelineExecution_ParallelStageElement() throws IOException {
    StageElement stageElement = StageElement.builder().identifier("testIdentifier").build();
    ServiceYaml entity = ServiceYaml.builder().identifier("serviceIdentifier").name("serviceName").build();
    DeploymentStage deploymentStage =
        DeploymentStage.builder()
            .identifier("testIdentifier")
            .serviceConfig(ServiceConfig.builder()
                               .service(entity)
                               .serviceDefinition(ServiceDefinition.builder().type("Kubernetes").build())
                               .build())
            .infrastructure(
                PipelineInfrastructure.builder()
                    .environment(EnvironmentYaml.builder().identifier("envIdentifier").name("stageName").build())
                    .build())
            .build();
    stageElement.setStageType(deploymentStage);
    ParallelStageElement parallelStageElement =
        ParallelStageElement.builder().sections(Lists.newArrayList(stageElement)).build();

    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    NgPipeline ngPipeline = NgPipeline.builder().stage(parallelStageElement).build();
    pipelineExecutionHelper.addStageSpecificDetailsToPipelineExecution(
        pipelineExecutionSummary, ngPipeline, Maps.of("testIdentifier", "node1"));

    CDStageExecutionSummary executionSummary = CDStageExecutionSummary.builder()
                                                   .planNodeId("node1")
                                                   .stageIdentifier("testIdentifier")
                                                   .executionStatus(ExecutionStatus.NOT_STARTED)
                                                   .build();
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().size()).isEqualTo(1);
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().get(0))
        .isInstanceOf(ParallelStageExecutionSummary.class);
    ParallelStageExecutionSummary parallelStageExecutionSummary =
        (ParallelStageExecutionSummary) pipelineExecutionSummary.getStageExecutionSummarySummaryElements().get(0);

    assertThat(parallelStageExecutionSummary.getStageExecutionSummaries().size()).isEqualTo(1);
    assertThat(parallelStageExecutionSummary.getStageExecutionSummaries().get(0)).isEqualTo(executionSummary);
    assertThat(pipelineExecutionSummary.getEnvIdentifiers()).isEqualTo(new ArrayList<>());
    assertThat(pipelineExecutionSummary.getServiceIdentifiers()).isEqualTo(new ArrayList<>());
    assertThat(pipelineExecutionSummary.getStageIdentifiers()).isEqualTo(Lists.newArrayList("testIdentifier"));
    assertThat(pipelineExecutionSummary.getStageTypes()).isEqualTo(Lists.newArrayList(DEPLOYMENT_STAGE_TYPE));
    assertThat(pipelineExecutionSummary.getServiceDefinitionTypes()).isEqualTo(new ArrayList<>());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetCDStageExecutionSummary() {
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid("id")
                                      .status(Status.FAILED)
                                      .failureInfo(FailureInfo.newBuilder().setErrorMessage("invalid").build())
                                      .startTs(123L)
                                      .endTs(124L)
                                      .build();

    CDStageExecutionSummary expectedExecutionSummary =
        CDStageExecutionSummary.builder()
            .nodeExecutionId("id")
            .executionStatus(ExecutionStatus.FAILED)
            .startedAt(123L)
            .endedAt(124L)
            .errorInfo(ExecutionErrorInfo.newBuilder().setMessage("invalid").build())
            .build();

    CDStageExecutionSummary actualStageExecutionSummary =
        pipelineExecutionHelper.getCDStageExecutionSummary(nodeExecution);

    assertThat(actualStageExecutionSummary).isEqualTo(expectedExecutionSummary);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetCDStageExecutionSummaryStatusUpdate() {
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid("id")
                                      .status(Status.FAILED)
                                      .failureInfo(FailureInfo.newBuilder().setErrorMessage("invalid").build())
                                      .startTs(123L)
                                      .endTs(124L)
                                      .build();

    PipelineExecutionHelper.StageIndex stageIndex =
        PipelineExecutionHelper.StageIndex.builder().firstLevelIndex(0).build();
    String key = String.format(
        "%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements, stageIndex.getFirstLevelIndex());
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.executionStatus, ExecutionStatus.FAILED)
        .set(key + "." + CDStageExecutionSummaryKeys.nodeExecutionId, nodeExecution.getUuid())
        .set(key + "." + CDStageExecutionSummaryKeys.startedAt, nodeExecution.getStartTs())
        .set(key + "." + CDStageExecutionSummaryKeys.endedAt, nodeExecution.getEndTs())
        .set(key + "." + CDStageExecutionSummaryKeys.errorInfo, nodeExecution.getFailureInfo());

    Update actualUpdate = pipelineExecutionHelper.getCDStageExecutionSummaryStatusUpdate(stageIndex, nodeExecution);

    assertThat(actualUpdate).isEqualTo(update);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetCDStageExecutionSummaryServiceUpdate() {
    ServiceExecutionSummary serviceExecutionSummary = ServiceExecutionSummary.builder().identifier("test").build();

    PipelineExecutionHelper.StageIndex stageIndex =
        PipelineExecutionHelper.StageIndex.builder().firstLevelIndex(0).build();
    String key = String.format(
        "%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements, stageIndex.getFirstLevelIndex());
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.serviceIdentifier, serviceExecutionSummary.getIdentifier())
        .set(key + "." + CDStageExecutionSummaryKeys.serviceExecutionSummary, serviceExecutionSummary)
        .addToSet(PipelineExecutionSummaryKeys.serviceIdentifiers, serviceExecutionSummary.getIdentifier());

    Update actualUpdate =
        pipelineExecutionHelper.getCDStageExecutionSummaryServiceUpdate(stageIndex, serviceExecutionSummary);

    assertThat(actualUpdate).isEqualTo(update);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetCDStageExecutionSummaryEnvironmentUpdate() {
    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder().build();

    PipelineExecutionHelper.StageIndex stageIndex =
        PipelineExecutionHelper.StageIndex.builder().firstLevelIndex(0).build();
    String key = String.format(
        "%s.%s", PipelineExecutionSummaryKeys.stageExecutionSummarySummaryElements, stageIndex.getFirstLevelIndex());
    Update update = new Update();
    update.set(key + "." + CDStageExecutionSummaryKeys.envIdentifier, environmentOutcome.getIdentifier())
        .addToSet(PipelineExecutionSummaryKeys.envIdentifiers, environmentOutcome.getIdentifier());

    Update actualUpdate =
        pipelineExecutionHelper.getCDStageExecutionSummaryEnvironmentUpdate(stageIndex, environmentOutcome);

    assertThat(actualUpdate).isEqualTo(update);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdatePipelineExecutionStatus() {
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();

    pipelineExecutionHelper.updatePipelineExecutionStatus(pipelineExecutionSummary,
        NodeExecution.builder()
            .node(PlanNodeProto.newBuilder().setUuid("node1").build())
            .status(Status.ERRORED)
            .build());
    assertThat(pipelineExecutionSummary.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testMapArtifactsOutcomeToSummary() {
    ServiceOutcome.ArtifactsOutcome artifactsOutcome =
        ServiceOutcome.ArtifactsOutcome.builder()
            .primary(DockerArtifactOutcome.builder()
                         .primaryArtifact(true)
                         .imagePath("image")
                         .tag("tag")
                         .type(ArtifactSourceType.DOCKER_HUB.getDisplayName())
                         .build())
            .sidecars(Maps.of("sidecar1", DockerArtifactOutcome.builder().imagePath("image1").tag("tag1").build()))
            .build();
    ServiceExecutionSummary.ArtifactsSummary artifactsSummary =
        ServiceExecutionSummary.ArtifactsSummary.builder()
            .primary(DockerArtifactSummary.builder().imagePath("image").tag("tag").build())
            .sidecar(DockerArtifactSummary.builder().imagePath("image1").tag("tag1").build())
            .build();

    ServiceExecutionSummary.ArtifactsSummary result = pipelineExecutionHelper.mapArtifactsOutcomeToSummary(
        ServiceOutcome.builder().artifactsResult(artifactsOutcome).build());
    assertThat(result).isEqualTo(artifactsSummary);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testFindStageExecutionSummaryByNodeExecutionId() {
    // Serial
    CDStageExecutionSummary stageExecutionSummary1 = CDStageExecutionSummary.builder().nodeExecutionId("node1").build();
    CDStageExecutionSummary stageExecutionSummary2 = CDStageExecutionSummary.builder().nodeExecutionId("node2").build();
    CDStageExecutionSummary stageExecutionSummary3 = CDStageExecutionSummary.builder().nodeExecutionId("node3").build();
    CDStageExecutionSummary stageExecutionSummary4 = CDStageExecutionSummary.builder().nodeExecutionId("node4").build();

    List<StageExecutionSummary> stageExecutionSummaryList =
        Arrays.asList(stageExecutionSummary1, stageExecutionSummary2, stageExecutionSummary3, stageExecutionSummary4);

    CDStageExecutionSummary foundStageExecutionSummary =
        pipelineExecutionHelper.findStageExecutionSummaryByNodeExecutionId(stageExecutionSummaryList, "node2");
    assertThat(foundStageExecutionSummary).isEqualTo(stageExecutionSummary2);

    foundStageExecutionSummary =
        pipelineExecutionHelper.findStageExecutionSummaryByNodeExecutionId(stageExecutionSummaryList, "invalid");
    assertThat(foundStageExecutionSummary).isEqualTo(null);

    // Parallel
    ParallelStageExecutionSummary parallelStageExecutionSummary =
        ParallelStageExecutionSummary.builder()
            .stageExecutionSummaries(Arrays.asList(stageExecutionSummary2, stageExecutionSummary3))
            .build();

    stageExecutionSummaryList =
        Arrays.asList(stageExecutionSummary1, parallelStageExecutionSummary, stageExecutionSummary4);

    foundStageExecutionSummary =
        pipelineExecutionHelper.findStageExecutionSummaryByNodeExecutionId(stageExecutionSummaryList, "node2");
    assertThat(foundStageExecutionSummary).isEqualTo(stageExecutionSummary2);

    foundStageExecutionSummary =
        pipelineExecutionHelper.findStageExecutionSummaryByNodeExecutionId(stageExecutionSummaryList, "invalid");
    assertThat(foundStageExecutionSummary).isEqualTo(null);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testFindStageIndexByPlanNodeId() {
    // Serial
    CDStageExecutionSummary stageExecutionSummary1 = CDStageExecutionSummary.builder().planNodeId("planNode1").build();
    CDStageExecutionSummary stageExecutionSummary2 = CDStageExecutionSummary.builder().planNodeId("planNode2").build();
    CDStageExecutionSummary stageExecutionSummary3 = CDStageExecutionSummary.builder().planNodeId("planNode3").build();
    CDStageExecutionSummary stageExecutionSummary4 = CDStageExecutionSummary.builder().planNodeId("planNode4").build();

    List<StageExecutionSummary> stageExecutionSummaryList =
        Arrays.asList(stageExecutionSummary1, stageExecutionSummary2, stageExecutionSummary3, stageExecutionSummary4);

    PipelineExecutionHelper.StageIndex stageIndex =
        pipelineExecutionHelper.findStageIndexByPlanNodeId(stageExecutionSummaryList, "planNode2");
    assertThat(stageIndex)
        .isEqualTo(PipelineExecutionHelper.StageIndex.builder().firstLevelIndex(1).secondLevelIndex(-1).build());

    stageIndex = pipelineExecutionHelper.findStageIndexByPlanNodeId(stageExecutionSummaryList, "invalid");
    assertThat(stageIndex).isEqualTo(null);

    // Parallel
    ParallelStageExecutionSummary parallelStageExecutionSummary =
        ParallelStageExecutionSummary.builder()
            .stageExecutionSummaries(Arrays.asList(stageExecutionSummary2, stageExecutionSummary3))
            .build();

    stageExecutionSummaryList =
        Arrays.asList(stageExecutionSummary1, parallelStageExecutionSummary, stageExecutionSummary4);

    stageIndex = pipelineExecutionHelper.findStageIndexByPlanNodeId(stageExecutionSummaryList, "planNode2");
    assertThat(stageIndex)
        .isEqualTo(PipelineExecutionHelper.StageIndex.builder().firstLevelIndex(1).secondLevelIndex(0).build());

    stageIndex = pipelineExecutionHelper.findStageIndexByPlanNodeId(stageExecutionSummaryList, "invalid");
    assertThat(stageIndex).isEqualTo(null);
  }
}
