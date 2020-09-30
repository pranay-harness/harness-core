package io.harness.cdng.pipeline.executions;

import static io.harness.cdng.pipeline.DeploymentStage.DEPLOYMENT_STAGE_TYPE;
import static io.harness.rule.OwnerRule.SAHIL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.executions.beans.CDStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.ParallelStageExecutionSummary;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import org.apache.groovy.util.Maps;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class PipelineExecutionHelperTest extends CDNGBaseTest {
  @Inject PipelineExecutionHelper pipelineExecutionHelper;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testAddStageSpecificDetailsToPipelineExecution_StageElement() throws IOException {
    StageElement stageElement = StageElement.builder().identifier("testIdentifier").build();
    DeploymentStage deploymentStage =
        DeploymentStage.builder()
            .identifier("testIdentifier")
            .service(ServiceConfig.builder()
                         .identifier(ParameterField.createValueField("serviceIdentifier"))
                         .serviceDefinition(ServiceDefinition.builder().type("Kubernetes").build())
                         .build())
            .infrastructure(PipelineInfrastructure.builder()
                                .environment(EnvironmentYaml.builder()
                                                 .identifier(ParameterField.createValueField("envIdentifier"))
                                                 .name(ParameterField.createValueField("stageName"))
                                                 .build())
                                .build())
            .build();
    stageElement.setStageType(deploymentStage);
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    NgPipeline ngPipeline = NgPipeline.builder().stage(stageElement).build();
    pipelineExecutionHelper.addStageSpecificDetailsToPipelineExecution(
        pipelineExecutionSummary, ngPipeline, Maps.of("testIdentifier", "node1"));

    CDStageExecutionSummary executionSummary = CDStageExecutionSummary.builder()
                                                   .planNodeId("node1")
                                                   .serviceIdentifier("serviceIdentifier")
                                                   .serviceDefinitionType("Kubernetes")
                                                   .envIdentifier("envIdentifier")
                                                   .stageIdentifier("testIdentifier")
                                                   .executionStatus(ExecutionStatus.NOT_STARTED)
                                                   .build();
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().size()).isEqualTo(1);
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().get(0)).isEqualTo(executionSummary);
    assertThat(pipelineExecutionSummary.getEnvIdentifiers()).isEqualTo(Lists.newArrayList("envIdentifier"));
    assertThat(pipelineExecutionSummary.getServiceIdentifiers()).isEqualTo(Lists.newArrayList("serviceIdentifier"));
    assertThat(pipelineExecutionSummary.getStageIdentifiers()).isEqualTo(Lists.newArrayList("testIdentifier"));
    assertThat(pipelineExecutionSummary.getStageTypes()).isEqualTo(Lists.newArrayList(DEPLOYMENT_STAGE_TYPE));
    assertThat(pipelineExecutionSummary.getServiceDefinitionTypes()).isEqualTo(Lists.newArrayList("Kubernetes"));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testAddStageSpecificDetailsToPipelineExecution_ParallelStageElement() throws IOException {
    StageElement stageElement = StageElement.builder().identifier("testIdentifier").build();
    DeploymentStage deploymentStage =
        DeploymentStage.builder()
            .identifier("testIdentifier")
            .service(ServiceConfig.builder()
                         .identifier(ParameterField.createValueField("serviceIdentifier"))
                         .serviceDefinition(ServiceDefinition.builder().type("Kubernetes").build())
                         .build())
            .infrastructure(PipelineInfrastructure.builder()
                                .environment(EnvironmentYaml.builder()
                                                 .identifier(ParameterField.createValueField("envIdentifier"))
                                                 .name(ParameterField.createValueField("stageName"))
                                                 .build())
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
                                                   .serviceIdentifier("serviceIdentifier")
                                                   .serviceDefinitionType("Kubernetes")
                                                   .envIdentifier("envIdentifier")
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
    assertThat(pipelineExecutionSummary.getEnvIdentifiers()).isEqualTo(Lists.newArrayList("envIdentifier"));
    assertThat(pipelineExecutionSummary.getServiceIdentifiers()).isEqualTo(Lists.newArrayList("serviceIdentifier"));
    assertThat(pipelineExecutionSummary.getStageIdentifiers()).isEqualTo(Lists.newArrayList("testIdentifier"));
    assertThat(pipelineExecutionSummary.getStageTypes()).isEqualTo(Lists.newArrayList(DEPLOYMENT_STAGE_TYPE));
    assertThat(pipelineExecutionSummary.getServiceDefinitionTypes()).isEqualTo(Lists.newArrayList("Kubernetes"));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdateStageExecutionStatus() {
    CDStageExecutionSummary executionSummary = CDStageExecutionSummary.builder()
                                                   .planNodeId("node1")
                                                   .serviceIdentifier("serviceIdentifier")
                                                   .serviceDefinitionType("Kubernetes")
                                                   .envIdentifier("envIdentifier")
                                                   .stageIdentifier("testIdentifier")
                                                   .executionStatus(ExecutionStatus.NOT_STARTED)
                                                   .build();
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();
    pipelineExecutionSummary.addStageExecutionSummaryElement(executionSummary);

    pipelineExecutionHelper.updateStageExecutionStatus(pipelineExecutionSummary,
        NodeExecution.builder().node(PlanNode.builder().uuid("node1").build()).status(Status.ERRORED).build());
    assertThat(pipelineExecutionSummary.getStageExecutionSummarySummaryElements().size()).isEqualTo(1);
    assertThat(((CDStageExecutionSummary) pipelineExecutionSummary.getStageExecutionSummarySummaryElements().get(0))
                   .getExecutionStatus())
        .isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testUpdatePipelineExecutionStatus() {
    PipelineExecutionSummary pipelineExecutionSummary = PipelineExecutionSummary.builder().build();

    pipelineExecutionHelper.updatePipelineExecutionStatus(pipelineExecutionSummary,
        NodeExecution.builder().node(PlanNode.builder().uuid("node1").build()).status(Status.ERRORED).build());
    assertThat(pipelineExecutionSummary.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }
}