package io.harness.cdng.pipeline.service;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Lists;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.executions.PipelineExecutionStatus;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.pipeline.executions.beans.CDStageExecution;
import io.harness.cdng.pipeline.executions.beans.ParallelStageExecution;
import io.harness.cdng.pipeline.executions.beans.PipelineExecution;
import io.harness.cdng.pipeline.executions.beans.StageExecution;
import io.harness.cdng.pipeline.executions.beans.dto.PipelineExecutionDTO;
import io.harness.cdng.pipeline.mappers.ExecutionToDtoMapper;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.engine.OrchestrationService;
import io.harness.exception.GeneralException;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.yaml.core.Artifact;
import io.harness.yaml.utils.YamlPipelineUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class NgPipelineExecutionServiceImpl implements NgPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;

  @Override
  public PlanExecution triggerPipeline(
      String pipelineYaml, String accountId, String orgId, String projectId, EmbeddedUser user) {
    final CDPipeline cdPipeline;
    try {
      cdPipeline = YamlPipelineUtils.read(pipelineYaml, CDPipeline.class);
      Map<String, Object> contextAttributes = new HashMap<>();

      final Plan planForPipeline =
          executionPlanCreatorService.createPlanForPipeline(cdPipeline, accountId, contextAttributes);
      return orchestrationService.startExecution(planForPipeline,
          ImmutableMap.of(SetupAbstractionKeys.accountId, accountId, SetupAbstractionKeys.orgIdentifier, orgId,
              SetupAbstractionKeys.projectIdentifier, projectId),
          user != null ? user : getEmbeddedUser());
    } catch (IOException e) {
      throw new GeneralException("error while de-serializing Yaml", e);
    }
  }

  @Override
  public List<PipelineExecutionDTO> getExecutions(
      String accountId, String orgId, String projectId, Criteria criteria, Pageable pageable) {
    return Lists.newArrayList(ExecutionToDtoMapper.writeExecutionDto(CreateDummyPipelineExecution()));
  }

  private PipelineExecution CreateDummyPipelineExecution() {
    String artifactIdentifier = "artifactIdentifier";
    List<StageExecution> stageExecutions = new ArrayList<>();
    stageExecutions.add(
        createDummyStageExecution("parallelStage1", artifactIdentifier, PipelineExecutionStatus.RUNNING));
    stageExecutions.add(
        createDummyStageExecution("parallelStage2", artifactIdentifier, PipelineExecutionStatus.RUNNING));

    ParallelStageExecution parallelStageExecution =
        ParallelStageExecution.builder().stageExecutions(stageExecutions).build();

    CDStageExecution cdStageDoneExecution =
        createDummyStageExecution("cdStageDone", artifactIdentifier, PipelineExecutionStatus.SUCCESS);

    return PipelineExecution.builder()
        .pipelineIdentifier("dummyPipelineIdentifier")
        .pipelineName("dummyPipeline")
        .planExecutionId("planExecutionId")
        .stageExecutionSummaryElements(Lists.newArrayList(cdStageDoneExecution, parallelStageExecution))
        .endedAt(10L)
        .envIdentifiers(new ArrayList<>())
        .triggeredBy(io.harness.ng.core.user.User.builder().name("admin").build())
        .triggerType(TriggerType.MANUAL)
        .serviceDefinitionTypes(Lists.newArrayList(ServiceDefinitionType.KUBERNETES))
        .startedAt(0L)
        .serviceIdentifiers(Lists.newArrayList("ServiceIdentifier1"))
        .stageIdentifiers(Lists.newArrayList("stageIdentifier"))
        .pipelineExecutionStatus(PipelineExecutionStatus.RUNNING)
        .build();
  }

  private CDStageExecution createDummyStageExecution(
      String postId, String artifactIdentifier, PipelineExecutionStatus pipelineExecutionStatus) {
    Artifact artifact = Artifact.builder().identifier(artifactIdentifier).build();
    return CDStageExecution.builder()
        .artifactsDeployed(Lists.newArrayList(artifact))
        .deploymentType(ServiceDefinitionType.KUBERNETES)
        .endedAt(10L)
        .startedAt(0L)
        .envIdentifier("environmentIdentifier" + postId)
        .planExecutionId("planExecutionId" + postId)
        .serviceIdentifier("serviceIdentifier" + postId)
        .pipelineExecutionStatus(pipelineExecutionStatus)
        .stageName("dev")
        .stageIdentifier("stageIdentifier" + postId)
        .build();
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}
