package io.harness.impl;

import static io.harness.beans.executionargs.ExecutionArgs.EXEC_ARGS;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.CIBuild;
import io.harness.core.ci.services.CIBuildServiceImpl;
import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.logging.AutoLogContext;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.util.CIExecutionAutoLogContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CIPipelineExecutionServiceImpl implements CIPipelineExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private CIBuildServiceImpl ciBuildService;

  public PlanExecution executePipeline(
      NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs, Long buildNumber) {
    Map<String, Object> contextAttributes = new HashMap<>();
    contextAttributes.put(EXEC_ARGS, ciExecutionArgs);

    try (AutoLogContext ignore =
             new CIExecutionAutoLogContext(ngPipelineEntity.getIdentifier(), ngPipelineEntity.getProjectIdentifier(),
                 ciExecutionArgs.getBuildNumberDetails().getBuildNumber().toString(),
                 ngPipelineEntity.getOrgIdentifier(), ngPipelineEntity.getAccountId(), OVERRIDE_ERROR)) {
      log.info("Started plan creation for pipeline from execution source: {}",
          ciExecutionArgs.getExecutionSource().getType());
      Plan plan = executionPlanCreatorService.createPlanForPipeline(
          ngPipelineEntity.getNgPipeline(), ngPipelineEntity.getAccountId(), contextAttributes);

      // TODO set user before execution which will be available once we build authentication
      // User user = UserThreadLocal.get()
      Map<String, String> setupAbstractions = new HashMap<>();
      setupAbstractions.put("accountId", ngPipelineEntity.getAccountId());
      setupAbstractions.put("pipelineId", ngPipelineEntity.getIdentifier());
      setupAbstractions.put("orgIdentifier", ngPipelineEntity.getOrgIdentifier());
      setupAbstractions.put("projectIdentifier", ngPipelineEntity.getProjectIdentifier());
      setupAbstractions.put("userEmail", "harsh.jain@harness.io");
      setupAbstractions.put("userId", "harsh");
      setupAbstractions.put("userName", "harsh jain");
      setupAbstractions.put("buildNumber", ciExecutionArgs.getBuildNumberDetails().getBuildNumber().toString());

      log.info("Started pipeline execution from execution source: {}", ciExecutionArgs.getExecutionSource().getType());
      PlanExecution planExecution = orchestrationService.startExecution(plan, setupAbstractions,
          ExecutionMetadata.newBuilder()
              .setRunSequence(0)
              .setTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                  .setTriggeredBy(TriggeredBy.newBuilder()
                                                      .putExtraInfo("email", "harsh.jain@harness.io")
                                                      .setIdentifier("harsh jain")
                                                      .setUuid("harsh")
                                                      .build())
                                  .setTriggerType(MANUAL)
                                  .build())
              .build());
      log.info("Submitted pipeline execution for build Number {} with planExecutionId: {}",
          ciExecutionArgs.getBuildNumberDetails().getBuildNumber(), planExecution.getUuid());
      createCIBuild(ngPipelineEntity, ciExecutionArgs, planExecution, buildNumber);
      return planExecution;
    }
  }

  private CIBuild createCIBuild(NgPipelineEntity ngPipelineEntity, CIExecutionArgs ciExecutionArgs,
      PlanExecution planExecution, Long buildNumber) {
    CIBuild ciBuild = CIBuild.builder()
                          .buildNumber(buildNumber)
                          .orgIdentifier(ngPipelineEntity.getOrgIdentifier())
                          .executionSource(ciExecutionArgs.getExecutionSource())
                          .projectIdentifier(ngPipelineEntity.getProjectIdentifier())
                          .pipelineIdentifier(ngPipelineEntity.getIdentifier())
                          .inputSet(ciExecutionArgs.getInputSet())
                          .triggerTime(System.currentTimeMillis()) // TODO Generate time at entry point
                          .accountIdentifier(ngPipelineEntity.getAccountId())
                          .executionId(planExecution.getUuid())
                          .build();

    log.info("Created CI Build number {} for pipeline execution from execution source: {}", ciBuild.getBuildNumber(),
        ciExecutionArgs.getExecutionSource().getType());

    return ciBuildService.save(ciBuild);
  }
}
